/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.pmease.commons.xmt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
Copyright 2010 Robin Shen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
**/

public class MigrationHelper {
	private static Logger logger = Logger.getLogger("com.pmease.commons.bmt.MigrationHelper");
	
	private static final Pattern migrateMethodPattern = Pattern.compile("migrate(\\d+)");
	
	// caches the analysis result to speed up migration process (there might be many 
	// bean data need to be migrated, such as various repository/builder/step 
	// definitions, etc).
	private static Map<String, MigratorAnalyzeResult> migratorAnalyzeResults = 
		new ConcurrentHashMap<String, MigratorAnalyzeResult>();

	private static MigratorAnalyzeResult getMigratorAnalyzeResult(Class<?> migrator) {
		MigratorAnalyzeResult migratorAnalyzeResult = 
			migratorAnalyzeResults.get(migrator.getName());
		if (migratorAnalyzeResult == null) {
			final MigratorAnalyzeResult newMigratorAnalyzeResult = 
				new MigratorAnalyzeResult();

			Method[] methods = migrator.getDeclaredMethods();
			for (int i=0; i<methods.length; i++) {
				Method method = methods[i];
				int migrateVersion = getVersion(method);
				if (migrateVersion != 0) { 
                                        /**
                                         * modified by Thomas Oster in order to allow public methods
                                         * and not use setAccessible(true)
                                         */
					if (!Modifier.isStatic(method.getModifiers())) {
						newMigratorAnalyzeResult.getMigrateVersions()
								.put(method.getName(), migrateVersion);
						newMigratorAnalyzeResult.getMigrateMethods().add(method);
					} else {
						throw new RuntimeException("Migrate method should be declared " +
								"as a private non-static method.");
					}
				}
			}

			Collections.sort(newMigratorAnalyzeResult.getMigrateMethods(), 
					new Comparator<Method>() {

				public int compare(Method migrate_x, Method migrate_y) {
					return newMigratorAnalyzeResult.getMigrateVersions().get(migrate_x.getName()) - 
							newMigratorAnalyzeResult.getMigrateVersions().get(migrate_y.getName());
				}
				
			});
			migratorAnalyzeResults.put(migrator.getName(), newMigratorAnalyzeResult);
			return newMigratorAnalyzeResult;
		} else {
			return migratorAnalyzeResult;
		}
	}

	private static int getVersion(Method method) {
		Matcher matcher = migrateMethodPattern.matcher(method.getName());
		if (matcher.find()) {
			int migrateVersion = Integer.parseInt(matcher.group(1));
			if (migrateVersion == 0) {
				throw new RuntimeException("Invalid migrate method name: " + 
						method.getName());
			}
			return migrateVersion;
		} else 
			return 0;
	}

	/**
	 * Get version of specified migrator class.
	 * @param migrator
	 * @return
	 */
	public static String getVersion(Class<?> migrator) {
		List<String> versionParts = new ArrayList<String>();
		Class<?> current = migrator;
		while (current != null && current != Object.class) {
			versionParts.add(String.valueOf(
					getMigratorAnalyzeResult(current).getDataVersion()));
			current = current.getSuperclass();
		}
		Collections.reverse(versionParts);
		StringBuffer buffer = new StringBuffer();
		for (String part: versionParts)
			buffer.append(part).append(".");
		return buffer.substring(0, buffer.length()-1);
	}
	
	/**
	 * Migrate from specified version to current version using specified migrator with 
	 * specified custom data. Custom data will be passed to various "migratexxx" methods.
	 * @param fromVersion
	 * @param migrator
	 * @param customData
	 * @return true if data is migrated; false if data is of current version and does not 
	 * need a migration.
	 */
	public static boolean migrate(String fromVersion, Class<?> migrator, 
			Object customData) {
		Stack<Integer> versionParts = new Stack<Integer>();
		for (String part: fromVersion.split("\\."))
			versionParts.push(Integer.valueOf(part));
		
		boolean migrated = false;
		
		Class<?> current = migrator;
		while (current != null && current != Object.class) {
			MigratorAnalyzeResult migratorAnalyzeResult = 
				getMigratorAnalyzeResult(current);
			
			int version;
			if (!versionParts.empty())
				version = versionParts.pop();
			else 
				version = 0;
			int size = migratorAnalyzeResult.getMigrateMethods().size();
			int start;
			if (version != 0) {
				start = size;
				for (int i=0; i<size; i++) {
					Method method = migratorAnalyzeResult.getMigrateMethods().get(i);
					if (method.getName().equals("migrate" + version)) {
						start = i;
						break;
					}
				}
				if (start == size) {
					String message = String.format("Can not find migrate method (migrator: %s, method: %s)", 
							current.getName(), "migrate" + version + "(Object)");
					logger.warning(message);
				} else {
					start++;
				}
			} else {
				start = 0;
			}
			
			for (int i=start; i<size; i++) {
				Method migrateMethod = migratorAnalyzeResult.getMigrateMethods().get(i);
				int previousVersion;
				if (i != 0) {
					Method previousMigrateMethod =
						migratorAnalyzeResult.getMigrateMethods().get(i-1);
					previousVersion = migratorAnalyzeResult.getMigrateVersions()
							.get(previousMigrateMethod.getName());
				} else {
					previousVersion = 0;
				}
				int currentVersion = migratorAnalyzeResult.getMigrateVersions()
						.get(migrateMethod.getName());
				String message = String.format("Migrating data (migrator: %s, from version: %s, " +
						"to version: %s)", current.getName(), String.valueOf(previousVersion), 
						String.valueOf(currentVersion));
				logger.fine(message);
				try {
					migrateMethod.invoke(migrator.newInstance(), customData, versionParts);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				} catch (InvocationTargetException e) {
					throw new RuntimeException(e);
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				}
				migrated = true;
			}
			current = current.getSuperclass();
		}
		return migrated;
	}	
}
