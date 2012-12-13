package car

class DriveController {

    private static String fileName="/tmp/command"
    private static int direction=0

    def index() { }

    def goForward() {
     new File(fileName).write("Forward\n")
     render 'Forward'
    }

    def goBackward() {
     new File(fileName).write("Backward\n")
     render 'Backword'
    }

    def goRight() {
     if (++direction > 1) direction = 1;
     switch (direction) {
     	case 1:
		new File(fileName).write("Right\n")
     		render 'Right'
		break;
	case 0:
		new File(fileName).write("Center\n")
                render 'Center'
                break;
	default: 
		render "Default"
     }
    }

    def goLeft() {
     if(--direction < -1) direction = -1;
     switch (direction) {
	case -1:
     		new File(fileName).write("Left\n")
     		render 'Left'
		break;
	case 0: 
		new File(fileName).write("Center\n")
                render 'Center'
                break; 
	default:
		render "Default"
     }
    }

    def goStop() {
     new File(fileName).write("Stop\n")
     render 'Stopped'     
    }
         
    def changeSpeed(String speed) {
     new File("/tmp/speed").write("${speed}\n")
     render speed
//     render '$speed'
    }
}
