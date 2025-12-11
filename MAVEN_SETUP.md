# Maven Configuration Guide

## Problem
If you encounter errors like:
```
Non-resolvable parent POM ... failed to transfer from https://repo.latitc.corp-apps.com/repository/public-maven
```

This means Maven is trying to use a corporate repository that's not accessible.

## Solution

### Option 1: Use the Project Settings File (Recommended)

Use the `temp-settings.xml` file included in this project:

```bash
mvn clean install -s temp-settings.xml
mvn spring-boot:run -s temp-settings.xml
mvn test -s temp-settings.xml
```

### Option 2: Clear Maven Cache and Force Update

Clear the cached failure and force Maven to update:

```bash
# Remove cached Spring Boot parent POM
rm -rf ~/.m2/repository/org/springframework/boot/spring-boot-starter-parent/3.1.5

# Build with update flag
mvn clean install -U -s temp-settings.xml
```

### Option 3: Create Local Maven Settings

Create or update `~/.m2/settings.xml` to override corporate settings:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <mirrors>
        <mirror>
            <id>central</id>
            <name>Maven Central</name>
            <url>https://repo1.maven.org/maven2</url>
            <mirrorOf>*,!internal.mirror</mirrorOf>
        </mirror>
    </mirrors>
</settings>
```

## Quick Commands

All commands should use the `-s temp-settings.xml` flag:

```bash
# Build
mvn clean install -s temp-settings.xml

# Run tests
mvn test -s temp-settings.xml

# Run application
mvn spring-boot:run -s temp-settings.xml

# Skip tests during build
mvn clean install -s temp-settings.xml -DskipTests
```

## Verification

After running with `temp-settings.xml`, you should see downloads from:
```
Downloading from central: https://repo1.maven.org/maven2/...
```

Instead of:
```
Downloading from internal.mirror: https://repo.latitc.corp-apps.com/...
```

