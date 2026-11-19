# Binding

A document is bound to one **controller** object. Expressions read its fields/getters, handlers call its methods,
`v-model` writes back.

```java
public final class SettingsController {
    public final State<Float> volume = new State<>(0.5f);           // reactive
    public final ListState<Friend> friends = new ListState<>();     // reactive list
    public final Computed<Integer> online = Computed.of(() -> (int) friends.get().stream().filter(Friend::online).count());
    public String version = "1.0";                                   // plain field (see "untracked")
    private boolean advanced;

    public boolean isAdvanced() { return advanced; }                 // getter
    public void setAdvanced(boolean on) { advanced = on; }           // setter for v-model
    public void reset() { volume.set(0.5f); }                        // @click="reset"
    public void remove(Friend f) { friends.remove(f); }              // @click="remove(f)"
}
```

```html
<slider v-model="volume"/>
<text>{{ online }} online · v{{ version }}</text>
<toggle v-model="advanced"/>
<row v-for="f in friends" :key="f.id"><text>{{ f.name }}</text><button @click="remove(f)">×</button></row>
```

## How names resolve

For `name` (and for `obj.name` on any value):

1. `Model` → its entry; `Map` → `map.get(name)`
2. `length`/`size` on strings, lists, maps, arrays
3. method `getName()`, `isName()`, `name()` (zero args), then field `name` – public or not, inherited too
4. if the value is a `State`, `ListState` or `Computed`, its current value is used (and tracked)

Inside templates, local names come first: `v-for` variables, slot props, component props, `$event`, then the
controller, then builtin functions.

Calls `open(m)` find a controller method by name and **argument count**; arguments are converted to the parameter
types (numbers → `int`/`float`/…, anything → `String`, truthiness → `boolean`, names → enums). Lookups are resolved
once per class and cached as `MethodHandle`s; there is no reflection while evaluating.

## Reactivity

| Type | Use |
|---|---|
| `State<T>` | `get()` (tracked), `set(v)` (notifies if different), `update(fn)`, `peek()` (untracked), `notifyChanged()` |
| `ListState<T>` | `add`, `add(i, x)`, `set`, `remove(i)`, `remove(x)`, `removeIf`, `move`, `replaceAll`, `clear`, `notifyChanged`; `get()` = live tracked view, `size()`, `get(i)`, `snapshot()` |
| `Computed<T>` | lazy cached value of a formula, recomputed when its inputs change |
| `Effect` + `Scheduler` | the machinery: every binding is an effect; changed effects are queued and run in the next `doc.frame()` |

A binding re-runs only when a value it read changed. Changes are applied in batches at the start of
`doc.frame(dt)`, then styles and layout update for the affected elements only.

**Untracked values**: plain fields, getters and map entries cannot notify. Bindings that read them are re-evaluated
after every template event handler (Vue-like) and when you call `doc.refresh()` – call it after changing plain data
from outside (a network callback, a tick handler).

Threading: vui is single-threaded. Change state on the thread that calls `frame`/`render` (in Minecraft: the
client/render thread).

## v-model write-back

`v-model="path"` writes the control's new value to `path`, in this order of support:
`Model` value · `Map` entry · a `State` field/getter (`state.set`) · setter `setName(x)` · non-final field.
Values are converted to the target type. A path that cannot be written reports
`cannot write 'name' (hint: add a setter, a non-final field or a State<?> named 'name')`.

## Model – binding without classes

```java
Model model = new Model();
ListState<Friend> friends = model.list("friends", loadFriends());   // returns the ListState
model.value("title", "Friends")
        .computed("count", () -> friends.size())
        .action("refresh", () -> reload())           // @click="refresh"
        .action1("open", friend -> show(friend))      // @click="open(f)"
        .action("add", args -> addFriend((String) args[0]));
State<String> title = model.state("title");          // reactive handle
Document doc = vui.open("mymod:friends", model);
```

`value(name, v)` creates or updates a `State`; `get(name)` reads it; `has`, `names`, `entry` inspect. Every Model
value is reactive.

## Components

Props declared in `<props>` are local variables of the component. Static attributes set them (converted to the
default's type), `:prop` bindings keep them in sync (kebab-case `item-size` matches prop `itemSize`). Names a
component does not declare fall back to the page's controller. `$emit('picked', value)` calls the usage's
`@picked` handler with `$event = value`. `v-model` on a component binds prop `value` and listens to
`update:value` (or `v-model:name` → prop `name`). Other attributes – `class`, `style`, `id`, events, unknown
attributes – go to the component's root element.

## Translations and functions

```java
vui.functions().translator((key, args) -> I18n.get(key, args));   // t('menu.title'), i18n('x', n)
vui.functions().register("shortName", args -> abbreviate(args[0])); // {{ shortName(user) }}
```

## Errors

Errors while evaluating (unknown method, `cannot read 'name' of null`, an exception in your getter) are reported
once per expression position into `doc.diagnostics()` and the error overlay; the binding yields `null` and the UI
keeps running. Use `a?.b` or `a && a.b` for optional values.

## Limits

- No deep reactivity: changing a field inside a list item only updates bindings that read a `State` there (or
  after handlers/`refresh()`).
- Overloads with the same argument count: the first found wins.
- `Model` and controllers are not thread-safe.
