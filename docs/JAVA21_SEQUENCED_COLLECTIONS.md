# Java 21 Sequenced Collections — Migration Guide

This document shows safe patterns for migrating to Java 21 `SequencedCollection` APIs.

## Before → After

### Accessing first/last elements

```java
// Before (Java 8+)
String first = list.get(0);
String last  = list.get(list.size() - 1);

// After (Java 21)
String first = list.getFirst();
String last  = list.getLast();
```

### Reversed iteration

```java
// Before
ListIterator<E> it = list.listIterator(list.size());
while (it.hasPrevious()) { E e = it.previous(); ... }

// After
for (E e : list.reversed()) { ... }
```

### Unmodifiable wrappers

```java
// Before
Collections.unmodifiableList(list)
Collections.unmodifiableSet(set)
Collections.unmodifiableMap(map)

// After (preferred for defensive copies)
List.copyOf(list)
Set.copyOf(set)
Map.copyOf(map)
// Note: copyOf() rejects null elements/keys/values — use the old API if nulls are valid.
```

### LinkedHashSet / LinkedHashMap

These now implement `SequencedSet` / `SequencedMap` in Java 21:

```java
SequencedSet<String> tags = new LinkedHashSet<>();
tags.addFirst("urgent");                // ← new in 21
String oldest = tags.getFirst();
SequencedSet<String> rev = tags.reversed();
```

## Where to Apply in Propertize

| File                                          | Pattern           |        Safe to Migrate         |
| --------------------------------------------- | ----------------- | :----------------------------: |
| `RbacService.unmodifiableSet()`               | → `Set.copyOf()`  |       ✅ Yes (no nulls)        |
| `RbacService.unmodifiableList()`              | → `List.copyOf()` |             ✅ Yes             |
| `NotificationSenderFactory.unmodifiableSet()` | → `Set.copyOf()`  |             ✅ Yes             |
| `OnboardingService.get(0)`                    | → `.getFirst()`   |             ✅ Yes             |
| Test files `.get(0)`                          | → `.getFirst()`   | ✅ Yes (optional, readability) |

## Checklist for Reviewers

- [ ] No null elements exist in the collection before calling `copyOf()`
- [ ] `getFirst()` / `getLast()` always called on non-empty lists (or guarded by `isEmpty()`)
- [ ] `reversed()` does not break downstream consumers expecting the original order
