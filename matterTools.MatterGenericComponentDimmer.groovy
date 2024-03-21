metadata {
    definition(name: "Matter Generic Component Dimmer", namespace: "matterTools", author: "jvm", component: true) {
        capability "Actuator"
        capability "Refresh"
        capability "Switch"
		capability "SwitchLevel"
        capability "ChangeLevel"
        
        command "on"  , [[name: "Remain on for (seconds)", type:"NUMBER", description:"Turn off the device after the specified number of seconds"]]
        command "setLevel"  , [[name: "Level*", type:"NUMBER", description:"Level to set (0 to 100)"],
                               [name: "Duration", type:"NUMBER", description:"Transition duration in seconds"], 
                               [name: "Remain on for (seconds)", type:"NUMBER", description:"Turn off the device after the specified number of seconds"]
                              ]
        command "toggleOnOff" 
        command "toggleOnStartup"

        // Identify Cluster
        attribute "IdentifyTime", "number"
        attribute "IdentifyType", "string"
        
        // Switch Cluster
        attribute "OnTime", "number"
        attribute "OffWaitTime", "number"
        attribute "StartUpOnOff", "string"
        
        // Level Cluster
        attribute "OnOffTransitionTime", "number"
        attribute "OnTransitionTime", "number"
        attribute "OffTransitionTime", "number"

        attribute "RemainingTime", "number"
        attribute "MinLevel", "number"
        attribute "MaxLevel", "number"
        attribute "OnLevel", "number"
        attribute "DefaultMoveRate", "number"
        
    }
    preferences {
        input(name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true)
        input(name: "StartUpOnOff", type: "enum", title:"<b>When Powered On State</b>", description: "<i>Set state when power is first applied to device</i>", options:StartUpOnOffEnum)
        input(name: "minOnLevel", type: "number", title:"<b>Minimum Turn On Value from Hubitat</b>", description: "<i>Minimum Power Level When Switching from Off to On. Only affects Hubitat controller On command.</i>")
    }
}
import groovy.transform.Field
import hubitat.helper.HexUtils
import hubitat.matter.DataType

@Field static Map StartUpOnOffEnum = [ 0:"Off", 1:"On", 2:"Toggle"]

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
}

// This parse routine handles Hubitat SendEvent type messages (not the description raw strings originating from the device). 
// Hubitat's convntion is to include a parse() routine with this function in Generic Component drivers (child device drivers).
// It accepts a List of one or more SendEvent-type Maps and operates to determine how those Hubitat sendEvent Maps should be handled.
// The List of SendEvent Maps may include event Maps that are not needed by a particular driver (as determined based on the attributes of the driver)
// and those "extra" Maps are discarded. This allows a more generic "event Map" producting method (e.g., matterTools.createListOfMatterSendEventMaps)
void parse(List sendEventTypeOfEvents) {
    try {
		List updateLocalStateOnlyAttributes = ["OnOffTransitionTime", "OnTransitionTime", "OffTransitionTime", "MinLevel", "MaxLevel", 
                                               "DefaultMoveRate", "OffWaitTime", "OnLevel", "Binding", "UserLabelList", "FixedLabelList", "VisibleIndicator", 
                                               "DeviceTypeList", "ServerList", "ClientList", "PartsList", "TagList"]
		sendEventTypeOfEvents.each {
			if (device.hasAttribute (it.name)) {
				if (txtEnable) {
					if(device.currentValue(it.name) == it.value) {
						log.info ((it.descriptionText) ? (it.descriptionText) : ("${device.displayName}: ${it.name} set to ${it.value}") )+" (unchanged)" // Log if txtEnable and the value is the same
					} else {
						log.info ((it.descriptionText) ? (it.descriptionText) : ("${device.displayName}: ${it.name} set to ${it.value}") ) // Log if txtEnable and the value is the same
					}
				}
                sendEvent(it)
            } else if (updateLocalStateOnlyAttributes.contains(it.name)) {
                device.updateDataValue(it.name, "${it.value}")
			}
		}
		// Always check and reset the color name after any update. 
		// In reality, only need to do it after a hue, saturation, or color temperature change, 
		// but for code simplicity, just let sendEvent handle that filtering!
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing parse with inputs ${sendEventTypeOfEvents}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } 
}

void updated() {
    Integer StartUpOnOff = settings?.StartUpOnOff as Integer
    if (StartUpOnOff && (StartUpOnOffEnum.get(StartUpOnOff) != device.currentValue("StartUpOnOff"))){
        log.info "updating, Changing Startup State from ${device.currentValue("StartUpOnOff")} to ${StartUpOnOffEnum.get(StartUpOnOff)}"
         List<Map<String, String>> attrWriteRequests = []
            attrWriteRequests.add(matter.attributeWriteRequest(getEndpoint(), 0x0006, 0x4003, DataType.INT8, HexUtils.integerToHexString(StartUpOnOff, 1) ))
        log.debug "Write Requests are: " + attrWriteRequests
         sendHubCommand(new hubitat.device.HubAction(matter.writeAttributes(attrWriteRequests), hubitat.device.Protocol.MATTER))
    }
}

void refresh() { parent?.refreshMatter(ep: getEndpoint() ) }

Integer getEndpoint() { Integer.parseInt(getDataValue("endpointId") ) }

void on() { 
    if (minOnLevel && device.currentValue("level") && (device.currentValue("switch") == "off")) {
        Integer onLevel = Math.max(minOnLevel as Integer, device.currentValue("level") as Integer)
        parent?.setLevel(ep: getEndpoint(), level:onLevel)
    } else {
        parent?.on(ep: getEndpoint() ) 
    }
}
void on(turnOffAfterSeconds) {   parent?.onWithTimedOff(ep:getEndpoint(), onTime10ths:(turnOffAfterSeconds * 10 as Integer) )}

void off() { parent?.off(ep: getEndpoint() ) }
void toggleOnOff() { parent?.toggleOnOff(ep: getEndpoint()) }
void OnWithTimedOff(timeInSeconds, guardTime = 0) {
    parent?.onWithTimedOff(ep: getEndpoint(), 
                           onTime10ths:(timeInSeconds * 10) as Integer, 
                           ((timeInSeconds + guardTime) * 10) as Integer)
}
                                         
                                         
void setLevel(level, ramp = null, onTime = null ) { parent?.setLevel(ep: getEndpoint(), 
                                                                     level:level as Integer, 
                                                                     transitionTime10ths: ramp.is(null) ? null : (ramp* 10) as Integer, 
                                                                     onTime10ths: onTime.is(null) ? null : (onTime * 10) as Integer 
                                                                    ) 
                                                  }

void startLevelChange(direction) { parent?.startLevelChange(ep: getEndpoint(), direction:direction) }

void stopLevelChange() { parent?.stopLevelChange(ep: getEndpoint()) }
