/*
 * Copyright (c) 2019  GeorgH93
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package at.pcgamingfreaks.BadRabbit.Bukkit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BadRabbit extends JavaPlugin
{
	@Override
	public void onLoad()
	{
		try
		{
			if(!(Bukkit.getPluginManager() instanceof SimplePluginManager))
			{
				getLogger().warning("[BadRabbit] Unknown plugin manager detected! Disabling plugin.");
				return;
			}

			//region init reflection variables
			final Class<?> pluginClassLoader = Class.forName("org.bukkit.plugin.java.PluginClassLoader");
			final Field pluginClassLoaderPlugin = getField(pluginClassLoader, "plugin");
			//endregion

			final SimplePluginManager spm = (SimplePluginManager) Bukkit.getPluginManager();

			//region unregister plugin
			pluginClassLoaderPlugin.set(getClassLoader(), null); // set the plugin to null in the class loader
			getField(pluginClassLoader, "pluginInit").set(getClassLoader(), null); // set the plugin init to null in the class loader
			@SuppressWarnings("unchecked") List<Plugin> plugins = (List<Plugin>) getField(SimplePluginManager.class, "plugins").get(spm);
			int index = plugins.indexOf(this);
			//endregion

			JavaPlugin newPluginInstance = createInstance(); // setup new plugin instance

			//region set refs to new plugin instance
			pluginClassLoaderPlugin.set(getClassLoader(), newPluginInstance);
			plugins.set(index, newPluginInstance);
			@SuppressWarnings("unchecked") Map<String, Plugin> lookup = (Map<String, Plugin>) getField(SimplePluginManager.class, "lookupNames").get(spm);
			lookup.replace(getDescription().getName(), this, newPluginInstance);
			lookup.replace(getDescription().getName().toLowerCase(Locale.ENGLISH), this, newPluginInstance); // Paper and forks
			try
			{
				getDescription().getClass().getMethod("getProvides");
				for(String provides : getDescription().getProvides())
				{
					lookup.replace(provides, this, newPluginInstance);
					lookup.replace(provides.toLowerCase(Locale.ENGLISH), this, newPluginInstance); // Paper and forks
				}
			}
			catch(NoSuchMethodException ignored) {} // the plugin description does not implement the getProvides method (old server versions)
			//endregion

			newPluginInstance.onLoad(); // call load event on new plugin instance

			if(detectPlugMan())
			{
				getLogger().warning("[BadRabbit] Please do not load this plugin using PlugMan! Is might cause problems or does not load correctly!");
				getField(JavaPlugin.class, "isEnabled").set(this, true);
				Bukkit.getPluginManager().enablePlugin(newPluginInstance);
			}
		}
		catch(Exception e)
		{
			getLogger().warning("[BadRabbit] Failed switching to real plugin!");
			e.printStackTrace();
		}
	}

	/**
	 * Checks if PlugMan has been used to load the plugin.
	 *
	 * @return True if PlugMan has been used to lad the plugin.
	 */
	private static boolean detectPlugMan()
	{
		StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		boolean plugman = false;
		for(StackTraceElement stackTraceElement : stackTrace)
		{
			if(stackTraceElement.getClassName().contains("com.rylinaux.plugman"))
			{
				plugman = true;
				break;
			}
		}
		return plugman;
	}

	@Override
	public void onEnable()
	{
		getLogger().warning("[BadRabbit] Failed to enable plugin.");
		setEnabled(false);
	}

	/**
	 * Gets a field reference from a class.
	 *
	 * @param clazz The class containing the field.
	 * @param name The name of the field.
	 * @return The field reference. Null if it was not found.
	 * @throws Exception If the field was not found or could not be modified to be accessible.
	 */
	protected static @NotNull Field getField(@NotNull Class<?> clazz, @NotNull String name) throws Exception
	{
		Field field = clazz.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	/**
	 * Initiate the instance of your plugin here.
	 *
	 * @return The instance of the plugin that should be used.
	 * @throws Exception If there is any problem preventing the init of the plugin instance.
	 */
	protected abstract @NotNull JavaPlugin createInstance() throws Exception;
}
