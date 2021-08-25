### OGC Testbed-17 Demo Server

OSH demo distribution deployed for OGC Testbed 17, including the following modules:

- Simulated ISA sensor network driver
- MISB video driver (playback from synthetic TS file)
- SensorML Process chain for georeferencing image frame (footprint) using MISB telemetry as input
- SensorML Process chain for tracking and georeferencing Video Moving Target (VMTI) using MISB video + telemetry as input

- SOS Service
- SensorThings Service with MQTT support
- SensorWeb API Service


#### Docker

To build the image, run:

`./gradlew docker`

To launch in a container locally:

`./gradlew dockerRun`

OSH will be available on port 8080 for testing

To push the image to GCR (Google Cloud Registry):

`./gradlew dockerPushGcr`





