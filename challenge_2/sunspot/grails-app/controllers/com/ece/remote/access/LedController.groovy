package com.ece.remote.access

class LedController {

    static Map<Integer, String> lightColors = new HashMap<Integer, String>() {
        {
            put(1, "BLUE")
            put(2, "BLUE")
            put(3, "BLUE")
            put(4, "BLUE")
            put(5, "BLUE")
            put(6, "BLUE")
            put(7, "BLUE")
            put(8, "WHITE")
        }
    };
    static int lightStatus = 1

    def index() {
        render(view:'index',
                model:[
                        status0: (lightStatus & 0x01) > 0 ? 'ON' : 'OFF',
                        status1: (lightStatus & 0x02) > 0 ? 'ON' : 'OFF',
                        status2: (lightStatus & 0x04) > 0 ? 'ON' : 'OFF',
                        status3: (lightStatus & 0x08) > 0 ? 'ON' : 'OFF',
                        status4: (lightStatus & 0x10) > 0 ? 'ON' : 'OFF',
                        status5: (lightStatus & 0x20) > 0 ? 'ON' : 'OFF',
                        status6: (lightStatus & 0x40) > 0 ? 'ON' : 'OFF',
                        status7: (lightStatus & 0x80) > 0 ? 'ON' : 'OFF'
                ])
    }


    def switchLight(int position) {
        def mask = (1 << position)
        def currentStatus = lightStatus & mask

        def file = new File("/Library/Tomcat/webapps/remote/commands.txt");
        FileWriter writer = new FileWriter(file);

        writer.write(String.valueOf(position) + "\n")
        writer.write(lightColors.get(position)+"\n")
        writer.close()

        if (currentStatus > 0) {
            lightStatus &= ~mask;
            render position
        } else {
            lightStatus |= mask;
            render 'ON'
        }


    }

    def getLightStatus(int position) {
        render (lightStatus & (1 << position)) > 0
    }


    def getLightColor(int position) {
        render lightColors.get(position)
    }

    def setLightColor(String color, int position) {
        lightColors.put(position, color)
    }
}
