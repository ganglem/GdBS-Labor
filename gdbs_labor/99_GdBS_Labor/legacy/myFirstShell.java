import static cTools.KernelWrapper.*;
import java.util.Scanner;
class myFirstShell {
public static void main(String[] args){
while(true){

	System.out.print("~:");
	Scanner sn = new Scanner(System.in);
	String command = new String();
	command = sn.nextLine();
	if(command.equals("exit")){
		exit(0);
	}
	//System.out.println(command);
	String[] commandArray = command.split(" ");
	String fileName = commandArray[0];
	int pid = fork();
	if(pid == 0){ 	// yay i'm a child
		execv(fileName, commandArray);
		exit(1);
	}
	else {
		int[] status = new int[1];
		status[0] = 0;
		waitpid(pid, status, 0);
	}
}
}
}
