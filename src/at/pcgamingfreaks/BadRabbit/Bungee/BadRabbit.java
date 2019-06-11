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

package at.pcgamingfreaks.BadRabbit.Bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.plugin.PluginManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public abstract class BadRabbit extends Plugin
{
	@Override
	public void onLoad()
	{
		try
		{
			Plugin newPluginInstance = createInstance(); // setup new plugin instance
			getMethod(Plugin.class, "init", ProxyServer.class, PluginDescription.class).invoke(newPluginInstance, getProxy(), getDescription());

			//region set refs to new plugin instance
			@SuppressWarnings("unchecked") Map<String, Plugin> plugins = (Map<String, Plugin>) getField(PluginManager.class, "plugins").get(getProxy().getPluginManager());
			plugins.put(getDescription().getName(), newPluginInstance);
			//endregion

			newPluginInstance.onLoad(); // call load event on new plugin instance
		}
		catch(Exception e)
		{
			getLogger().warning("[BadRabbit] Failed switching to real plugin!");
			e.printStackTrace();
		}
	}

	@Override
	public void onEnable()
	{
		getLogger().warning("[BadRabbit] Failed to enable plugin.");
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
	 * Gets a method reference from a class.
	 *
	 * @param clazz The class containing the method.
	 * @param name The name of the method.
	 * @param args The types of the parameters of the method.
	 * @return The method reference. Null if it was not found.
	 * @throws Exception If the method was not found or could not be modified to be accessible.
	 */
	public static @NotNull Method getMethod(@NotNull Class<?> clazz, @NotNull String name, @Nullable Class<?>... args) throws Exception
	{
		Method method = clazz.getDeclaredMethod(name, args);
		method.setAccessible(true);
		return method;
	}

	/**
	 * Initiate the instance of your plugin here.
	 *
	 * @return The instance of the plugin that should be used.
	 * @throws Exception If there is any problem preventing the init of the plugin instance.
	 */
	protected abstract @NotNull Plugin createInstance() throws Exception;
}