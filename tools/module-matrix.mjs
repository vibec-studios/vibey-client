import { readFileSync, readdirSync, writeFileSync } from 'node:fs';

const root = new URL('../', import.meta.url);
const read = path => readFileSync(new URL(path, root), 'utf8');
const modulesPath = 'gradle/modules.gradle';
const rulesPath = 'docs/SERVER-RULES.md';

function readModules() {
    const source = read(modulesPath).replace(/\/\/[^\r\n]*/g, '');
    const block = source.match(/ext\.clientModules\s*=\s*\[([\s\S]*?)^\][ \t]*\r?$/m)?.[1];
    if (!block) throw new Error(`Missing clientModules in ${modulesPath}`);
    const entries = /\s*(?:'([\w.]+)'|(\w+))\s*:\s*(?:'(all|fabric|neoforge)'|\[((?:\s*'[\w.-]+'\s*,?)+)\s*\])\s*(?:,|$)/gy;
    const modules = new Map();
    while (block.slice(entries.lastIndex).trim()) {
        const position = entries.lastIndex;
        const entry = entries.exec(block);
        if (!entry) throw new Error(`Unsupported module declaration near ${block.slice(position, position + 60)}`);
        const [, quoted, unquoted, selector, list] = entry;
        const id = quoted ?? unquoted;
        if (modules.has(id)) throw new Error(`Duplicate module ${id}`);
        modules.set(id, selector ?? Array.from(list.matchAll(/'([^']+)'/g), match => match[1]));
    }
    return modules;
}

function readTargets() {
    const releases = JSON.parse(read('gradle/mc-versions.json'));
    const entries = readdirSync(new URL('versions/', root), { withFileTypes: true });
    const targets = entries.filter(entry => entry.isDirectory() && /^(fabric|neoforge)-\d/.test(entry.name)).map(entry => {
        const [loader, line] = entry.name.split('-');
        if (!Array.isArray(releases[line])) throw new Error(`Missing release list for ${entry.name}`);
        return { id: entry.name, loader, line, minecraft: releases[line] };
    });
    return targets.sort((a, b) => a.loader.localeCompare(b.loader) || a.line.localeCompare(b.line, 'en', { numeric: true }));
}

function readRules(modules) {
    const block = read(rulesPath).match(/^## Keys\r?\n([\s\S]+?)^## /m)?.[1];
    if (!block) throw new Error(`Missing Keys section in ${rulesPath}`);
    const rules = [];
    const seen = new Set();
    for (const line of block.split(/\r?\n/).filter(line => line.startsWith('| `'))) {
        const [, keyCell, description, ownerCell, end] = line.split('|').map(cell => cell.trim());
        const keys = Array.from(keyCell.matchAll(/`([a-z0-9_.*-]+)`/g), match => match[1]);
        const owners = Array.from((ownerCell ?? '').matchAll(/`([\w.]+)`/g), match => match[1]);
        if (!keys.length || !description || !owners.length || end !== '') throw new Error(`Invalid rule row: ${line}`);
        for (const owner of owners) {
            if (owner !== 'core' && !modules.has(owner)) throw new Error(`Unknown rule owner ${owner}`);
        }
        for (const key of keys) {
            if (seen.has(key)) throw new Error(`Duplicate rule key ${key}`);
            seen.add(key);
            rules.push({ key, description, modules: owners });
        }
    }
    if (!rules.length) throw new Error('No server rules found');
    return rules;
}

function included(modules, id, target) {
    const selector = modules.get(id);
    const matches = selector === 'all' || selector === target.loader
        || (Array.isArray(selector) && selector.includes(target.id));
    const dot = id.lastIndexOf('.');
    return matches && (dot === -1 || included(modules, id.slice(0, dot), target));
}

function buildMatrix() {
    const declarations = readModules();
    const targets = readTargets();
    const targetIds = new Set(targets.map(target => target.id));
    for (const [id, selector] of declarations) {
        if (Array.isArray(selector)) {
            for (const target of selector) {
                if (!targetIds.has(target)) throw new Error(`Unknown target ${target} in ${id}`);
            }
        }
        const dot = id.lastIndexOf('.');
        if (dot !== -1 && !declarations.has(id.slice(0, dot))) throw new Error(`Missing parent for ${id}`);
    }

    const rules = readRules(declarations);
    const modules = [...declarations.keys(), 'core'].sort().map(id => ({
        id,
        kind: id === 'core' ? 'shared' : id.includes('.') ? 'part' : 'module',
        targets: targets.filter(target => id === 'core' || included(declarations, id, target)).map(target => target.id),
        serverRules: rules.filter(rule => rule.modules.includes(id)).map(rule => rule.key),
    }));
    const byId = new Map(modules.map(module => [module.id, module]));
    for (const rule of rules) {
        const available = new Set(rule.modules.flatMap(id => byId.get(id).targets));
        rule.targets = targets.filter(target => available.has(target.id)).map(target => target.id);
    }
    return { schema: 1, targets, modules, serverRules: rules };
}

function markdown(matrix) {
    const targets = new Map(matrix.targets.map(target => [target.id, target]));
    const versions = (ids, loader) => ids.map(id => targets.get(id))
        .filter(target => target.loader === loader).flatMap(target => target.minecraft).join(', ') || '–';
    return [
        '# Modul-Kompatibilität und Server-Regeln',
        '',
        'Erzeugt mit `node tools/module-matrix.mjs` aus den Modul-Targets und dem [Regelprotokoll](SERVER-RULES.md).',
        '',
        'Die Tabelle zeigt, was jeder Build enthält und welche Funktionen ein Server über das Vibey-Protokoll einschränken kann. Die tatsächlichen Regeln legt der jeweilige Server fest.',
        '',
        '| Modul | Fabric (Minecraft) | NeoForge (Minecraft) | Regel-Keys |',
        '|---|---|---|---|',
        ...matrix.modules.map(module => {
            const rules = module.serverRules.map(key => '`' + key + '`').join(', ') || '–';
            return `| \`${module.id}\` | ${versions(module.targets, 'fabric')} | ${versions(module.targets, 'neoforge')} | ${rules} |`;
        }),
        '',
    ].join('\n');
}

if (process.argv.slice(2).some(arg => arg !== '--check')) throw new Error('Usage: node tools/module-matrix.mjs [--check]');
const matrix = buildMatrix();
const artifacts = {
    'docs/MODULE-MATRIX.json': JSON.stringify(matrix, null, 2) + '\n',
    'docs/MODULE-MATRIX.md': markdown(matrix),
};
for (const [path, contents] of Object.entries(artifacts)) {
    if (process.argv.includes('--check')) {
        if (read(path).replace(/\r\n/g, '\n') !== contents) throw new Error(`${path} is out of date. Run node tools/module-matrix.mjs`);
    } else {
        writeFileSync(new URL(path, root), contents);
    }
}
