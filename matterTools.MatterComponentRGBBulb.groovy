metadata {
    definition(name: "Matter Component RGB Bulb", namespace: "matterTools", author: "jvm", component: true) {
        capability "Bulb"
        capability "Refresh"
        capability "Switch"
		capability "SwitchLevel"
        capability "ChangeLevel"
        capability "ColorControl"
        capability "ColorMode"
        capability "ColorTemperature"
        
        command "CleanupData"
        
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
        
        
        // Color Cluster
        attribute "colorCapabilities", "string"
        attribute "ColorTemperatureMinKelvin", "number"
        attribute "ColorTemperatureMaxKelvin", "number"

		
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

import groovy.transform.Field

@Field static List updateOnlyAttributes = ["OnOffTransitionTime", "OnTransitionTime", "OffTransitionTime", 
                                           "colorCapabilities","colorTemperatureMin", "colorTemperatureMax", 
                                           "minLevel", "maxLevel"]
void CleanupData() {
    log.info "Clearing Up state data"
    this.state.clear()
}
void updated() {
    log.info "Updated..."
    log.warn "description logging is: ${txtEnable == true}"
}

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
}

void refresh() { parent?.componentRefresh(this.device) }

void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List description) {
    description.each {
        if (device.hasAttribute (it.name)) {
            if (txtEnable && (device.currentValue(it.name) != it.value)) log.info it.descriptionText // Log if txtEnable and the value is changing
            if (updateOnlyAttributes.contains(it.name)) {
                device.updateDataValue(it.name, "${it.value}")
            } else {
                sendEvent(it)
            }
        }
    }
}

void on() {  parent?.componentOn(this.device) }

void off() { parent?.componentOff(this.device) }

void setLevel(level) { parent?.componentSetLevel(this.device, level) }

void setLevel(level, ramp) { parent?.componentSetLevel(this.device, level, ramp) }

void startLevelChange(direction) { parent?.componentStartLevelChange(this.device, direction) }

void stopLevelChange() { parent?.componentStopLevelChange(this.device) }

void setColor(colormap){  parent?.componentSetColor(this.device, colormap) }

void setHue(hue) { parent?.componentSetHue(this.device, hue) }

void setSaturation(saturation) { parent?.componentSetSaturation(this.device, saturation) }

void setColorTemperature(colortemperature, level, transitionTime) { parent?.componentSetColorTemperature(this.device, colortemperature, level, transitionTime) }


