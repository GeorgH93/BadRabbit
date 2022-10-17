[ci]: https://ci.pcgamingfreaks.at/job/BadRabbit/
[ciImg]: https://ci.pcgamingfreaks.at/job/BadRabbit/badge/icon
[versionImg]: https://img.shields.io/maven-metadata/v?color=blue&label=version&metadataUrl=https%3A%2F%2Frepo.pcgamingfreaks.at%2Frepository%2Fmaven-releases%2Fat%2Fpcgamingfreaks%2FBadRabbit%2Fmaven-metadata.xml
[license]: https://github.com/GeorgH93/BadRabbit/blob/master/LICENSE
[licenseImg]: https://img.shields.io/github/license/GeorgH93/BadRabbit.svg

# BadRabbit
BadRabbit allows to switch the main Plugin class at plugin load time.


[![Build Status][ciImg]][ci] ![Version][versionImg] [![licenseImg]][license]


## Example
```java
public class MyPluginBadRabbit extends BadRabbit {
	@Override
	protected @NotNull JavaPlugin createInstance() throws Exception {
		return (myCondition()) ? new MyPluginMain1() : new MyPluginMain2();
	}
}
```

If you are looking for a real usage example, you can have a look at the Minepacks plugin [here](https://github.com/GeorgH93/Minepacks/blob/master/src/at/pcgamingfreaks/Minepacks/Bukkit/MinepacksBadRabbit.java).

## Maven
### Repository
```
<repository>
	<id>pcgf-repo</id>
	<url>https://repo.pcgamingfreaks.at/repository/maven-everything</url>
</repository>
```
### Dependency Bukkit
```
<dependency>
    <groupId>at.pcgamingfreaks</groupId>
    <artifactId>BadRabbit-Bukkit</artifactId>
    <version>1.11</version>
</dependency>
```
### Dependency BungeeCord
```
<dependency>
    <groupId>at.pcgamingfreaks</groupId>
    <artifactId>BadRabbit-Bungee</artifactId>
    <version>1.11</version>
</dependency>
```

## Build
### Requirements
* Java >= 7
* Maven >= 3.5

### Build it
```
mvn clean install
```


## Links
[Javadoc](https://ci.pcgamingfreaks.at/job/BadRabbit/javadoc/)