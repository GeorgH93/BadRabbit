# BadRabbit
BadRabbit allows to switch the main Plugin class at plugin load time.


## Example
```java
public class MyPluginBadRabbit extends BadRabbit {
	@Override
	protected @NotNull JavaPlugin createInstance() throws Exception {
		return (myCondition()) ? new MyPluginMain1() : new MyPluginMain2();
	}
}
```

If you are looking for a real usage example, you can have a look at the Minepacks plugin [here](https://github.com/GeorgH93/Minepacks/blob/master/src/at/pcgamingfreaks/Minepacks/Bukkit/MinepacksBadRabbit.java) .