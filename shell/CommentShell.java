// Import necessary libraries
import java.util.Scanner;
import java.io.File;
import java.util.Arrays;

// Define the shell class
class CommentShell {
  public static void main(String[] args) {
    
    // Get the PATH environment variable and split it into an array
    String env = System.getenv("PATH");
    String[] path = env.split(":");
    
    // Start an infinite loop for continuously receiving user input
    while (true) {
      
      // Split the user input into pipes, commands, and parameters
      String[][][] input = splitInput();
      
      // Set up variables for tracking pipe counts and I/O redirection
      int pipeCount = input.length - 1;
      int newStdin = STDIN_FILENO;
      int newStdout = STDOUT_FILENO;
      
      // Loop through each pipe in the input
      for (String[][] pipes : input) {
        
        // Set up a file descriptor for the pipe
        int[] pipeFd = {STDIN_FILENO, STDOUT_FILENO};
        if (pipeCount > 0) {
          if (pipe(pipeFd) < 0)
            System.err.println("ERROR: Pipe failed");
          pipeCount--;
        }
        
        // Loop through each command in the pipe
        for (String[] comm : pipes) {
          
          // Find the full path of the command executable
          returnValidPath(comm, path);
          
          // Parse any I/O redirection in the command
          String[][] files = parseRedirect(comm);
          
          // Create a new process for the command and set up I/O redirection
          if (createProcess(files[0], files[1][0], files[2][0], pipeFd, newStdin, newStdout) != 0) {
            break;
          }
        }
        
        // Set up I/O redirection for the next pipe
        newStdin = pipeFd[0];
        newStdout = pipeFd[1];
      }
    }
  }
  
  // Method for parsing I/O redirection in a command
  static String[][] parseRedirect(String[] arr) {
    int i;
    int indexOut = 0;
    int indexIn = 0;
    for (i = 0; i < arr.length; i++) {
      if (arr[i].equals(">"))
        indexOut = i;
      else if (arr[i].equals("<"))
        indexIn = i;
    }
    String[][] ret = new String[3][i];
    ret[1][0] = null;
    ret[2][0] = null;

    if (indexOut != 0) {
      ret[2][0] = arr[indexOut + 1];
    }
    if (indexIn != 0) {
      ret[1][0] = arr[indexIn + 1];  
    }

    if (indexOut > indexIn) {
      if (indexIn != 0)
        ret[0] = Arrays.copyOfRange(arr, 0, indexIn);
      else
        ret[0] = Arrays.copyOfRange(arr, 0, indexOut);
    } else if (indexOut < indexIn) {
      if (indexOut != 0)
        ret[0] = Arrays.copyOfRange(arr, 0, indexOut);
      else
        ret[0] = Arrays.copyOfRange(arr, 0, indexIn);
    } else 
      ret[0] = arr;
    
    return ret;
  }

  // Method for setting up I/O redirection for a command
  static int redirectOutput(String fout, String fin) {
    // Case: redirect stdout
    if (fout != null) {
        // Close stdout and open file in arr[i+1]
        close(STDOUT_FILENO); // 1 = stdout
        int fd;
        if ((fd = open(fout, O_WRONLY | O_CREAT)) < 0)
          return -1;
      }
      if (fin != null) {
        close(STDIN_FILENO);
        int fd;
        if ((fd = open(fin, O_RDONLY)) < 0)
          return -1;
      }
      return 0;
    } 
  
    // Method for finding the full path of a command executable
    static void returnValidPath(String[] arr, String[] env) {
      for (String s : env) {
        File tmp = new File(s + "/" + arr[0]);
        if (tmp.exists()) {
          arr[0] = s + "/" + arr[0];
          break;
        }
      }
    }
  
    // Method for splitting user input into pipes, commands, and parameters
    static String[][][] splitInput() {
      // Get user input from the command line
      Scanner in = new Scanner(System.in);
      System.out.print("shell> ");
      String input = in.nextLine();
  
      // Split the input into pipes, commands, and parameters
      String[] pipSep = input.split(" \\| ");
      String[][] sepInput = new String[pipSep.length][]; 
      for (int i = 0; i < pipSep.length; i++) {
        sepInput[i] = pipSep[i].split(" && ");
      }
      String[][][] splitInput = new String[sepInput.length][][];  
      for (int i = 0; i < sepInput.length; i++) {
        splitInput[i] = new String[sepInput[i].length][];
        for (int j = 0; j < sepInput[i].length; j++) {
          splitInput[i][j] = sepInput[i][j].split(" ");  
        }
      }
      
      // Print out the split input for debugging purposes
      for (String[][] d2 : splitInput) {
        for (String[] d1 : d2) {
          System.err.println(Arrays.toString(d1));
        }
      }
      
      return splitInput;
    }
  
    // Method for creating a new process for a command and setting up I/O redirection
    static int createProcess(String[] splitInput, String fin, String fout, int[] pipeFd, int newStdin, int newStdout) {
      // Check if the command is "exit", in which case exit the shell
      if (splitInput[0].equals("exit")) {
        System.err.println("\n");  
        exit(0);
      }
  
      // Set up variables for tracking the child process and its status
      int[] status = new int[1];
      int pidChild;
  
      // Fork a new process for the command
      if ((pidChild = fork()) < 0) {
        System.err.println("Error: fork");
        exit(1);
      }
  
      // If we're in the child process
      if (pidChild == 0) {
        
        // Set up I/O redirection for the command
        if (newStdin != STDIN_FILENO && newStdout != STDOUT_FILENO) {
          close(newStdout);
          dup2(newStdin, STDIN_FILENO);
          close(newStdin);
        }
        
        // Parse any I/O redirection in the command
        if (fout != null || fin != null) {
          if (redirectOutput(fout, fin) != 0)
          System.err.println("ERROR: Error redirecting output");
          return 1;
        }
      }
      
      // Set up I/O redirection for writing to the pipe
      if (pipeFd[0] != STDIN_FILENO && pipeFd[1] != STDOUT_FILENO) {
        close(pipeFd[0]);
        dup2(pipeFd[1], STDOUT_FILENO);
        close(pipeFd[1]);
      }

      // Execute the command
      if (execv(splitInput[0], splitInput) < 0) {
        System.err.println("ERROR: execv");
        exit(1);
      }
    
    // If we're in the parent process
    else {
      // Close file descriptors for the parent process
      if (newStdin != STDIN_FILENO && newStdout != STDOUT_FILENO) {
        close(newStdin);
        close(newStdout);
      }
      
      // Wait for the child process to finish
      if (waitpid(pidChild, status, 0) < 0) {
        System.err.println("ERROR: waiting for child");
        exit(1);
      }
      
      // Check if the child process returned an error code
      if (status[0] != 0) {
        System.err.println("LOG: Child returned error code");
        return 1;
      }
    } 
    return 0;
    }
}

  