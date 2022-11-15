import java.util.LinkedList;
import java.util.Scanner;

import static cTools.KernelWrapper.execv;
import static cTools.KernelWrapper.exit;
import static cTools.KernelWrapper.fork;
import static cTools.KernelWrapper.waitpid;

class secondShell {
  public static void main(String[] args)
  {
    while (true) {

      System.out.print("~: ");
      Scanner sn = new Scanner(System.in);
      String command = new String();
      command = sn.nextLine();
      if (command.equals("exit")) {
        exit(0);
      }
      //System.out.println(command);
      String[] rawArray = command.split(" && ");
      LinkedList<String[]> commandList = new LinkedList();
      for (int i = 0; i < rawArray.length; i++) {
        commandList.push(rawArray[i].split(" "));
      }


	/*for(int i =0; i<commandList.size(); i++){
        for(int j=0; j<commandList.get(i).length; j++){
			System.out.println(commandList.get(i)[j]);
		}
	}*/


      for (int i = 0; i < commandList.size(); i++) {
        int pid = fork();
        boolean error = false;
        if (pid == 0) {
          execv(commandList.get(i)[0].trim(), commandList.get(i));
        }
        else {
          int[] status = new int[1];
          status[0] = 0;
          if (waitpid(pid, status, 0) != 0) {
//				exit(1);
            break;
          }
        }
      }
    }
  }
}
