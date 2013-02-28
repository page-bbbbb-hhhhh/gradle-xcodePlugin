/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery

import org.gradle.api.tasks.TaskAction


class InfoPlistModifyTask extends AbstractXcodeTask {

	@TaskAction
	def prepare() {
		def infoPlist = getInfoPlist()
		println "Updating " + infoPlist

		if (project.infoplist.bundleIdentifier != null) {

			runCommand([
							"/usr/libexec/PlistBuddy",
							infoPlist,
							"-c",
							"Set :CFBundleIdentifier " + project.infoplist.bundleIdentifier
			])
		}

		// add suffix to bundleIdentifier
		if (project.infoplist.bundleIdentifierSuffix != null) {
			def bundleIdentifier = getValueFromPlist(infoPlist, "CFBundleIdentifier")

			runCommand([
							"/usr/libexec/PlistBuddy",
							infoPlist,
							"-c",
							"Set :CFBundleIdentifier " + bundleIdentifier + project.infoplist.bundleIdentifierSuffix
			])
		}

        // Modify bundle bundleDisplayName
        if (project.infoplist.bundleDisplayName != null) {

            runCommand([
                    "/usr/libexec/PlistBuddy",
                    infoPlist,
                    "-c",
                    "Set :CFBundleDisplayName " + project.infoplist.bundleDisplayName
            ])
        }

        // add suffix to bundleDisplayName
        if (project.infoplist.bundleDisplayNameSuffix != null) {
            def bundleDisplayName = getValueFromPlist(infoPlist, "CFBundleDisplayName")

            runCommand([
                    "/usr/libexec/PlistBuddy",
                    infoPlist,
                    "-c",
                    "Set :CFBundleDisplayName " + bundleDisplayName + project.infoplist.bundleDisplayNameSuffix
            ])
        }
        println "\nINFO: You will need to freshly install the app to see the change of bundleDisplayName!\n"

		println "project.infoplist.version: " + project.infoplist.version
		def version;
		if (project.infoplist.version != null) {
			version = project.infoplist.version
		} else {
			version = runCommandWithResult([
							"/usr/libexec/PlistBuddy",
							infoPlist,
							"-c",
							"Print :CFBundleVersion"])
		}

		if (project.infoplist.versionSuffix) {
			version = version + project.infoplist.versionSuffix
		}

		if (project.infoplist.versionPrefix) {
			version = project.infoplist.versionPrefix + version
		}

		println "Modify CFBundleVersion to " + version
		runCommand([
						"/usr/libexec/PlistBuddy",
						infoPlist,
						"-c",
						"Set :CFBundleVersion " + version])


		def shortVersionString
		try {
			shortVersionString = runCommandWithResult([
							"/usr/libexec/PlistBuddy",
							infoPlist,
							"-c",
							"Print :CFBundleShortVersionString"])
		} catch (IllegalStateException ex) {
			// no CFBundleShortVersionString exists so noting can be modified!
			return;
		}

		if (project.infoplist.shortVersionString != null) {
			shortVersionString = project.infoplist.shortVersionString
		}
		if (project.infoplist.shortVersionStringSuffix) {
			shortVersionString = shortVersionString + project.infoplist.shortVersionStringSuffix
		}

		if (project.infoplist.shortVersionStringPrefix) {
			shortVersionString = project.infoplist.shortVersionStringPrefix + shortVersionString
		}

		println "Modify CFBundleShortVersionString to " + shortVersionString
		runCommand([
						"/usr/libexec/PlistBuddy",
						infoPlist,
						"-c",
						"Set :CFBundleShortVersionString " + shortVersionString])

	}

}