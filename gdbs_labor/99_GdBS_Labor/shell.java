import java.io.File;
import java.util.Scanner;

import static cTools.KernelWrapper.O_CREAT;
import static cTools.KernelWrapper.O_RDONLY;
import static cTools.KernelWrapper.O_WRONLY;
import static cTools.KernelWrapper.STDIN_FILENO;
import static cTools.KernelWrapper.STDOUT_FILENO;
import static cTools.KernelWrapper.close;
import static cTools.KernelWrapper.dup2;
import static cTools.KernelWrapper.execv;
import static cTools.KernelWrapper.exit;
import static cTools.KernelWrapper.fork;
import static cTools.KernelWrapper.open;
import static cTools.KernelWrapper.pipe;
import static cTools.KernelWrapper.waitpid;

public class shell {
  static int[] newfd = new int[] {0, 1};
  static int[] oldfd = new int[] {0, 1};
  static int j;
  static int pipesLength;
  private static Scanner scan;

  public static void main(String[] args)
  {
    while (true) {
      scan = new Scanner(System.in);
      System.out.print("$: ");

      String input = scan.nextLine();

      String commands = input.trim();
      int status = 0;
      // split by pipes
      String[] pipes = commands.split("\\s*\\|\\s*");
      pipesLength = pipes.length;

      for (j = 0; j < pipes.length; j++) {
        String values[] = pipes[j].split("\\s+");
        if (!(input.isEmpty() || values.length == 0)) {

          String path = "";
          File prog = new File(values[0]);
          if (prog.isFile() && prog.canExecute()) {
            path = values[0];
          }
          else {
            String[] paths = System.getenv("PATH").split(":");
            for (String currentPath : paths) {
              File currentProg = new File(currentPath + "/" + prog.getName());
              if (currentProg.isFile() && currentProg.canExecute()) {
                prog = currentProg;
                break;
              }
            }
          }
          if (prog == null) {
            System.out.println("Program not found");
            status = -1;
          }
          else {
            path = prog.getAbsolutePath();
            if (j < pipesLength - 1) {
              pipe(newfd);
              status = run(path, values);
              close(newfd[1]);
              oldfd[0] = newfd[0];
            }
            else {
              int pid = fork();
              if (pid == 0) {
                if (oldfd[0] != STDIN_FILENO) {
                  dup2(oldfd[0], STDIN_FILENO);
                }
                if (values.length >= 3 && values[values.length - 2].equals(("<"))) {
                  close(STDIN_FILENO);
                  open(values[values.length - 1], O_RDONLY);
                  String[] values2 = new String[values.length - 2];
                  for (int k = 0; k < values.length - 2; k++) {
                    values2[k] = values[k];
                  }
                  values = values2;
                }
                else if (values.length >= 3 && values[values.length - 2].equals((">"))) {
                  close(STDOUT_FILENO);
                  open(values[values.length - 1], O_CREAT | O_WRONLY);
                  String[] values2 = new String[values.length - 2];
                  for (int k = 0; k < values.length - 2; k++) {
                    values2[k] = values[k];
                  }
                  values = values2;
                }
                execv(path, values);
              }
              else {
                int[] statusOk = new int[1];
                int error = waitpid(pid, statusOk, 0);
                if (error == -1) {
                  System.out.println("Error");
                }
              }
            }
          }
        }
      }
    }
  }

  private static int run(String path, String[] values)
  {
    int pid = fork();
    if (pid != 0) {
      int[] status = new int[1];
      int error = waitpid(pid, status, 0);
      if (error == -1) {
        System.out.println("Error");
      }
      return status[0];
    }
    else {
      if (oldfd[0] != STDIN_FILENO) {
        dup2(oldfd[0], STDIN_FILENO);
        close(oldfd[0]);
      }
      if (newfd[1] != STDOUT_FILENO) {
        dup2(newfd[1], STDOUT_FILENO);
        close(newfd[1]);
      }
      execv(path, values);
      System.out.println("error");
      exit(1);
    }
    return 0;
  }
}
