# Vibey Client (Media): the sound the PC plays, for the music effects of the sky.
# WASAPI loopback of the default output device (no permission, no driver needed). Nothing is stored or sent
# anywhere: writes "VMA1", the sample rate and a reserved int (little endian), then 16-bit mono PCM (about 12 kHz)
# to stdout, which only the game reads. Ends when the game ends or stops reading.
# Argument: the game's process id.
param([int]$GamePid = 0)
$ErrorActionPreference = 'Stop'

$source = @'
using System;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Threading;

namespace VibeyMedia
{
    [ComImport, Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")]
    class MMDeviceEnumeratorCom { }

    [ComImport, Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IMMDeviceEnumerator
    {
        [PreserveSig] int EnumAudioEndpoints(int dataFlow, int stateMask, out IntPtr devices);
        [PreserveSig] int GetDefaultAudioEndpoint(int dataFlow, int role, out IMMDevice device);
    }

    [ComImport, Guid("D666063F-1587-4E43-81F1-B948E807363F"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IMMDevice
    {
        [PreserveSig] int Activate(ref Guid iid, int clsCtx, IntPtr activationParams, [MarshalAs(UnmanagedType.IUnknown)] out object iface);
    }

    [ComImport, Guid("1CB9AD4C-DBFA-4c32-B178-C2F568A703B2"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IAudioClient
    {
        [PreserveSig] int Initialize(int shareMode, int streamFlags, long bufferDuration, long periodicity, IntPtr format, IntPtr sessionGuid);
        [PreserveSig] int GetBufferSize(out uint frames);
        [PreserveSig] int GetStreamLatency(out long latency);
        [PreserveSig] int GetCurrentPadding(out uint padding);
        [PreserveSig] int IsFormatSupported(int shareMode, IntPtr format, out IntPtr closest);
        [PreserveSig] int GetMixFormat(out IntPtr format);
        [PreserveSig] int GetDevicePeriod(out long defaultPeriod, out long minimumPeriod);
        [PreserveSig] int Start();
        [PreserveSig] int Stop();
        [PreserveSig] int Reset();
        [PreserveSig] int SetEventHandle(IntPtr handle);
        [PreserveSig] int GetService(ref Guid iid, [MarshalAs(UnmanagedType.IUnknown)] out object service);
    }

    [ComImport, Guid("C8ADBD64-E71E-48a0-A4DE-185C395CD317"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IAudioCaptureClient
    {
        [PreserveSig] int GetBuffer(out IntPtr data, out uint frames, out uint flags, out ulong devicePosition, out ulong qpcPosition);
        [PreserveSig] int ReleaseBuffer(uint frames);
        [PreserveSig] int GetNextPacketSize(out uint frames);
    }

    public static class Loopback
    {
        const int E_RENDER = 0, E_MULTIMEDIA = 1, CLSCTX_ALL = 23, SHARED = 0, LOOPBACK = 0x00020000, SILENT = 2;

        static void Check(int hr)
        {
            if (hr < 0) throw new COMException("WASAPI", hr);
        }

        public static int Run(int gamePid)
        {
            Stream stdout = Console.OpenStandardOutput();
            IMMDeviceEnumerator devices = (IMMDeviceEnumerator)new MMDeviceEnumeratorCom();
            IMMDevice device;
            Check(devices.GetDefaultAudioEndpoint(E_RENDER, E_MULTIMEDIA, out device));
            Guid clientId = typeof(IAudioClient).GUID;
            object o;
            Check(device.Activate(ref clientId, CLSCTX_ALL, IntPtr.Zero, out o));
            IAudioClient client = (IAudioClient)o;
            IntPtr format;
            Check(client.GetMixFormat(out format));
            int tag = Marshal.ReadInt16(format, 0) & 0xFFFF;
            int channels = Marshal.ReadInt16(format, 2);
            int rate = Marshal.ReadInt32(format, 4);
            int bits = Marshal.ReadInt16(format, 14);
            bool isFloat = tag == 3 || (tag == 0xFFFE && Marshal.ReadInt32(format, 24) == 3);
            Check(client.Initialize(SHARED, LOOPBACK, 2000000, 0, format, IntPtr.Zero));
            Guid captureId = typeof(IAudioCaptureClient).GUID;
            Check(client.GetService(ref captureId, out o));
            IAudioCaptureClient capture = (IAudioCaptureClient)o;

            int step = Math.Max(1, (int)Math.Round(rate / 12000.0));
            int outRate = rate / step;
            byte[] header = new byte[12];
            header[0] = (byte)'V'; header[1] = (byte)'M'; header[2] = (byte)'A'; header[3] = (byte)'1';
            BitConverter.GetBytes(outRate).CopyTo(header, 4);
            stdout.Write(header, 0, header.Length);
            stdout.Flush();

            Check(client.Start());
            float[] floats = new float[0];
            short[] shorts = new short[0];
            int[] ints = new int[0];
            byte[] output = new byte[16384];
            int outPos = 0;
            double acc = 0;
            int accCount = 0;
            Stopwatch clock = Stopwatch.StartNew();
            long lastCheck = 0;
            Process game = null;
            try { if (gamePid > 0) game = Process.GetProcessById(gamePid); } catch (Exception) { return 0; }
            while (true)
            {
                Thread.Sleep(10);
                if (clock.ElapsedMilliseconds - lastCheck > 1000)
                {
                    lastCheck = clock.ElapsedMilliseconds;
                    if (game != null) { game.Refresh(); if (game.HasExited) return 0; }
                }
                uint packet;
                Check(capture.GetNextPacketSize(out packet));
                while (packet > 0)
                {
                    IntPtr data; uint frames, flags; ulong devicePos, qpcPos;
                    Check(capture.GetBuffer(out data, out frames, out flags, out devicePos, out qpcPos));
                    int n = (int)frames * channels;
                    bool silent = (flags & SILENT) != 0;
                    if (!silent)
                    {
                        if (isFloat && bits == 32) { if (floats.Length < n) floats = new float[n]; Marshal.Copy(data, floats, 0, n); }
                        else if (bits == 16) { if (shorts.Length < n) shorts = new short[n]; Marshal.Copy(data, shorts, 0, n); }
                        else if (bits == 32) { if (ints.Length < n) ints = new int[n]; Marshal.Copy(data, ints, 0, n); }
                        else silent = true;
                    }
                    for (int f = 0; f < frames; f++)
                    {
                        double s = 0;
                        if (!silent)
                        {
                            int i0 = f * channels;
                            for (int c = 0; c < channels; c++)
                            {
                                if (isFloat) s += floats[i0 + c];
                                else if (bits == 16) s += shorts[i0 + c] / 32768.0;
                                else s += ints[i0 + c] / 2147483648.0;
                            }
                            s /= channels;
                        }
                        acc += s;
                        if (++accCount == step)
                        {
                            double v = acc / step;
                            acc = 0;
                            accCount = 0;
                            int iv = (int)Math.Round(Math.Max(-1.0, Math.Min(1.0, v)) * 32767.0);
                            output[outPos++] = (byte)iv;
                            output[outPos++] = (byte)(iv >> 8);
                            if (outPos == output.Length)
                            {
                                stdout.Write(output, 0, outPos);
                                outPos = 0;
                            }
                        }
                    }
                    Check(capture.ReleaseBuffer(frames));
                    Check(capture.GetNextPacketSize(out packet));
                }
                if (outPos > 0)
                {
                    try { stdout.Write(output, 0, outPos); stdout.Flush(); }
                    catch (IOException) { return 0; }
                    outPos = 0;
                }
            }
        }
    }
}
'@

try {
    Add-Type -TypeDefinition $source -Language CSharp | Out-Null
    exit [VibeyMedia.Loopback]::Run($GamePid)
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 2
}
