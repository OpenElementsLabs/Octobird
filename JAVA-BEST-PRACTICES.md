# Java Best Practices

General Java coding conventions for this project.

## Use `final` wherever possible

All fields, local variables, method parameters, catch parameters, and for-each loop variables
must be declared `final` wherever possible.

```java
// Method parameters
public void handle(final String event, final int count)

// Local variables
final String name = getValue();

// Catch parameters
catch (final IOException e)

// For-each loop variables
for (final String item : list)

// Constructor parameters
public MyClass(final int value)
```

Only omit `final` when the variable is intentionally reassigned:

```java
boolean authorized = false;
if (condition) {
    authorized = true;
}
```

Exceptions where `final` is not required:
- **Lambda parameters** — they are effectively final by default
- **Record component parameters** — they are inherently final
- **Interface method parameters** — `final` on abstract method signatures has no effect
