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

import com.google.common.io.ByteStreams;

import org.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static java.lang.Package.getPackage;

/**
 * A helper class that can be used to load classes without them being modified by bukkit.
 * It's also possible to override certain stages of the class loading process to modify the behaviour.
 * It only works for not loaded classes. The classes will be added to the given plugin class loader once loaded.
 */
public class PluginClassLoaderBypass
{
	private static final Class<?> PLUGIN_CLASS_LOADER;
	private static final Field FIELD_CLASSES;
	private static final Method SET_CLASS, GET_PACKAGE, DEFINE_CLASS, DEFINE_PACKAGE, DEFINE_PACKAGE_WITHOUT_MANIFEST;

	static
	{
		Class<?> pcl = null;
		Field classes = null;
		Method setClass = null, getPackage = null, defineClass = null, definePackage = null, definePackageNF = null;
		try
		{
			pcl = Class.forName("org.bukkit.plugin.java.PluginClassLoader");
			classes = getField(pcl, "classes");
			setClass = getMethod(JavaPluginLoader.class, "setClass", String.class, Class.class);
			getPackage = getMethod(ClassLoader.class, "getPackage", String.class);
			defineClass = getMethod(SecureClassLoader.class, "defineClass", String.class, byte[].class, int.class, int.class, CodeSource.class);
			definePackage = getMethod(URLClassLoader.class, "definePackage", String.class, Manifest.class, URL.class);
			definePackageNF = getMethod(ClassLoader.class, "definePackage", String.class, String.class, String.class, String.class, String.class, String.class, String.class, URL.class);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		PLUGIN_CLASS_LOADER = pcl;
		FIELD_CLASSES = classes;
		SET_CLASS = setClass;
		GET_PACKAGE = getPackage;
		DEFINE_CLASS = defineClass;
		DEFINE_PACKAGE = definePackage;
		DEFINE_PACKAGE_WITHOUT_MANIFEST = definePackageNF;
	}

	private final ClassLoader classLoader;
	private final JavaPluginLoader loader;
	private JarFile jar;
	private Manifest manifest;
	private URL url;

	public PluginClassLoaderBypass(ClassLoader classLoader) throws Exception
	{
		if(!classLoader.getClass().equals(PLUGIN_CLASS_LOADER)) throw new IllegalArgumentException("The given class loader must be a Bukkit PluginClassLoader!");
		this.classLoader = classLoader;
		loader = (JavaPluginLoader) getField(PLUGIN_CLASS_LOADER, "loader").get(classLoader);
		try
		{
			jar = (JarFile) getField(PLUGIN_CLASS_LOADER, "jar").get(classLoader);
			url = (URL) getField(PLUGIN_CLASS_LOADER, "url").get(classLoader);
			manifest = (Manifest) getField(PLUGIN_CLASS_LOADER, "manifest").get(classLoader);
		}
		catch(Exception e)
		{
			File file = (File) getField(PLUGIN_CLASS_LOADER, "file").get(classLoader);
			jar = new JarFile(file, true);
			manifest = jar.getManifest();
			url = file.toURI().toURL();
		}
	}

	public void loadClass(final String name) throws Exception
	{
		JarEntry entry = jar.getJarEntry(getPath(name));
		if (entry == null) throw new ClassNotFoundException(name);
		byte[] classBytes;
		try (InputStream stream = jar.getInputStream(entry))
		{
			classBytes = loadJarData(name, stream);
		}
		checkPackage(name);
		Class<?> result = buildClass(name, classBytes, entry);
		installClass(name, result);
	}

	protected Class<?> buildClass(@NotNull String name, byte[] classBytes, JarEntry entry) throws Exception
	{
		CodeSigner[] signers = entry.getCodeSigners();
		CodeSource source = new CodeSource(url, signers);
		return (Class<?>) DEFINE_CLASS.invoke(classLoader , name, classBytes, 0, classBytes.length, source);
	}

	protected void checkPackage(@NotNull String name) throws Exception
	{
		int dot = name.lastIndexOf('.');
		if (dot != -1)
		{
			String pkgName = name.substring(0, dot);

			if (GET_PACKAGE.invoke(classLoader, pkgName) == null) {
				try
				{
					if (manifest != null)
					{
						DEFINE_PACKAGE.invoke(classLoader, pkgName, manifest, url);
					}
					else
					{
						DEFINE_PACKAGE_WITHOUT_MANIFEST.invoke(classLoader, pkgName, null, null, null, null, null, null, null);
					}
				}
				catch (IllegalArgumentException ex)
				{
					if (getPackage(pkgName) == null)  throw new IllegalStateException("Cannot find package " + pkgName);
				}
			}
		}
	}

	protected void installClass(String name, Class<?> result) throws Exception
	{
		if (result == null) throw new ClassNotFoundException(name);
		SET_CLASS.invoke(loader, name, result);
		@SuppressWarnings("unchecked") Map<String, Class<?>> classes = (Map<String, Class<?>>) FIELD_CLASSES.get(classLoader);
		classes.put(name, result);
	}

	protected @NotNull String getPath(@NotNull String className)
	{
		return className.replace('.', '/').concat(".class");
	}

	protected byte[] loadJarData(@NotNull String name, @NotNull InputStream stream) throws Exception
	{
		//noinspection UnstableApiUsage
		return ByteStreams.toByteArray(stream);
	}

	//region reflection methods
	protected static @NotNull Field getField(@NotNull Class<?> clazz, @NotNull String name) throws Exception
	{
		Field field = clazz.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	protected static @NotNull Method getMethod(@NotNull Class<?> clazz, @NotNull String name, @Nullable Class<?>... args) throws Exception
	{
		Method method = clazz.getDeclaredMethod(name, args);
		method.setAccessible(true);
		return method;
	}
	//endregion
}