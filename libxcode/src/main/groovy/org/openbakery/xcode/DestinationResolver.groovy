package org.openbakery.xcode

import org.openbakery.simulators.SimulatorControl
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DestinationResolver {

	private static Logger logger = LoggerFactory.getLogger(DestinationResolver.class)

	SimulatorControl simulatorControl

	DestinationResolver(SimulatorControl simulatorControl) {
		this.simulatorControl = simulatorControl
	}

	List<Destination> allFor(XcodebuildParameters parameters) {
		if (parameters.type == Type.iOS && !parameters.simulator) {
			return []
		}
		return simulatorControl.getAllDestinations(parameters.type)
	}

	List<Destination> getDestinations(XcodebuildParameters parameters) {

		logger.debug("getAvailableDestinations for {}", parameters)
		def availableDestinations = []
		if (parameters.type == Type.macOS) {
			logger.debug("is macOS build")
			availableDestinations << new Destination("OS X", "OS X", "10.x")
			return availableDestinations
		}

		def allDestinations = allFor(parameters)
		def runtime = simulatorControl.getMostRecentRuntime(parameters.type)


		boolean isSimulatorBuild = isSimulatorBuild(parameters)
		logger.debug("isSimulatorBuild {}", isSimulatorBuild)

		if (isSimulatorBuild) {
			// filter only on simulator builds
			if (parameters.configuredDestinations != null) {
				logger.debug("checking destinations if they are available: {}", parameters.configuredDestinations)
				for (Destination destination in parameters.configuredDestinations) {

					if (destination.os == null) {
						destination.os = runtime.version.toString()
					}
					availableDestinations.addAll(findMatchingDestinations(destination, allDestinations, true))
				}

				logger.debug("availableDestinations {}", availableDestinations)


				// if no destination was found try to find a similar destination
				// e.g. is 'iPad Pro' was specified and a 'iPad Pro (11-inch)' is present, than this is used
				if (availableDestinations.size() == 0) {
					logger.debug("find similar destination")
					for (Destination destination in parameters.configuredDestinations) {
						if (destination.os == null) {
							destination.os = runtime.version.toString()
						}

						def similarDestination = findMatchingDestinations(destination, allDestinations, false)
						if (similarDestination.size() > 0) {
							availableDestinations.add(similarDestination.first())
							break
						}
					}
				}

				logger.debug("availableDestinations {}", availableDestinations)

				if (availableDestinations.isEmpty()) {
					logger.error("No matching simulators found for specified destinations: {}", parameters.configuredDestinations)
					throw new IllegalStateException("No matching simulators found!")
				}
			} else {

				logger.info("There was no destination configured that matches the available. Therefor all available destinations where taken.")

				switch (parameters.devices) {
					case Devices.PHONE:
						availableDestinations = allDestinations.findAll {
							d -> d.name.contains("iPhone")
						}
						break
					case Devices.PAD:
						availableDestinations = allDestinations.findAll {
							d -> d.name.contains("iPad")
						}
						break
					default:
						availableDestinations.addAll(allDestinations)
						break
				}
			}
		} else if (parameters.configuredDestinations != null) {
			logger.debug("is a device build so add all given device destinations")
			// on the device build add the given destinations
			availableDestinations.addAll(parameters.configuredDestinations)
		}


		logger.debug("availableDestinations: " + availableDestinations);

		return availableDestinations
	}

	private boolean isSimulatorBuild(XcodebuildParameters parameters) {
		boolean result = parameters.isSimulatorBuildOf(Type.iOS) || parameters.isSimulatorBuildOf(Type.tvOS)
		logger.debug("is simulator build: {}", result)
		return result
	}

	private List<Destination> findMatchingDestinations(Destination destination, List<Destination> allDestinations, boolean exact) {
		List<Destination> result = []

		logger.debug("finding matching destination for: {}", destination)

		for (Destination device in allDestinations) {
			if (!matches(destination.platform, device.platform)) {
				//logger.debug("{} does not match {}", device.platform, destination.platform);
				continue
			}
			if (exact) {
				if (!matches(destination.name, device.name)) {
					//logger.debug("{} does not match {}", device.name, destination.name);
					continue
				}
			} else {
				if (!startsWith(device.name, destination.name)) {
					//logger.debug("{} does not match {}", device.name, destination.name);
					continue
				}
			}

			if (!matches(destination.arch, device.arch)) {
				//logger.debug("{} does not match {}", device.arch, destination.arch);
				continue
			}
			if (!matches(destination.id, device.id)) {
				//logger.debug("{} does not match {}", device.id, destination.id);
				continue
			}
			if (!matches(destination.os, device.os)) {
				//logger.debug("{} does not match {}", device.os, destination.os);
				continue
			}

			logger.debug("FOUND matching destination: {}", device)

			result << device
		}
		logger.debug("finding matching destination result: {}", result)

		return result
	}


	private static boolean matches(String first, String second) {
		if (first != null && second == null) {
			return true
		}

		if (first == null && second != null) {
			return true
		}

		if (first == second) {
			return true
		}

		if (second.matches(first)) {
			return true
		}

		return false
	}

	private static boolean startsWith(String first, String second) {
		if (first != null && second == null) {
			return true
		}

		if (first == null && second != null) {
			return true
		}
		return first.startsWith(second)
	}
}
