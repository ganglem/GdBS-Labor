import static cTools.KernelWrapper.*;

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

class shell {

  public static void main(String[] args) {
    String env = System.getenv("PATH");
    String[] path = env.split(":");

    // Looping continuously for input
    while (true) {
      // Splitting input into pipes, commands, and parameters
      // [pipes][commands][parameters]
      String[][][] input = splitInput();

      // Counting the number of pipes
      int pipe_count = input.length - 1;
      // Initializing the standard input and output file descriptors
      int new_stdin = STDIN_FILENO;
      int new_stdout = STDOUT_FILENO;

      // Looping through each pipe
      for (String[][] pipes : input) {
        // Initializing the pipe file descriptors
        int[] pip_fd = { STDIN_FILENO, STDOUT_FILENO };
        if (pipe_count > 0) {
          // Creating a pipe
          if (pipe(pip_fd) < 0) System.err.println("ERROR: Pipe failed");
          pipe_count--;
        }
        // Setting up the standard input and output file descriptors
        for (String[] comm : pipes) {
          // Calling the method to return a valid path for the command
          return_valid_path(comm, path);
          // Parsing the redirection symbols
          String[][] files = parse_redirect(comm);
          // Creating a new process for the command
          if (
            create_proc(
              files[0],
              files[1][0],
              files[2][0],
              pip_fd,
              new_stdin,
              new_stdout
            ) !=
            0
          ) {
            break;
          }
        }
        // Updating the standard input and output file descriptors for the next pipe
        new_stdin = pip_fd[0];
        new_stdout = pip_fd[1];
      }
    }
  }

  //return String[][] (param,fin,fout)

  // Method to parse redirection symbols and return the input, output, and parameter files
  static String[][] parse_redirect(String[] arr) {
    int i;
    int index_out = 0;
    int index_in = 0;
    // Looping through the array to find the redirection symbols
    for (i = 0; i < arr.length; i++) {
      if (arr[i].equals(">")) index_out = i; else if (
        arr[i].equals("<")
      ) index_in = i;
    }
    // Creating a new 3x(i+1) array to store the input, output, and parameter files
    String[][] ret = new String[3][i + 1];
    // Initializing the output and input files to null
    ret[1][0] = null;
    ret[2][0] = null;

    // Checking if there is an output file
    if (index_out != 0) {
      ret[2][0] = arr[index_out + 1];
    }
    // Checking if there is an input file
    if (index_in != 0) {
      ret[1][0] = arr[index_in + 1];
    }

    // Separating the command and parameters from the input and output files
    if (index_out > index_in) {
      if (index_in != 0) ret[0] =
        Arrays.copyOfRange(arr, 0, index_in); else ret[0] =
        Arrays.copyOfRange(arr, 0, index_out);
    } else if (index_out < index_in) {
      if (index_out != 0) ret[0] =
        Arrays.copyOfRange(arr, 0, index_out); else ret[0] =
        Arrays.copyOfRange(arr, 0, index_in);
    } else ret[0] = arr;

    return ret;
  }

  // Method to redirect output to a file
  static int redirect_output(String fout, String fin) {
    // Checking if output redirection is required
    if (fout != null) {
      // Closing the standard output file descriptor and opening the output file
      close(STDOUT_FILENO); // 1 = stdout
      int fd;
      if ((fd = open(fout, O_WRONLY | O_CREAT)) < 0) return -1;
    }
    // Checking if input redirection is required
    if (fin != null) {
      // Closing the standard input file descriptor and opening the input file
      close(STDIN_FILENO);
      int fd;
      if ((fd = open(fin, O_RDONLY)) < 0) return -1;
    }
    return 0;
  }

  // Method to return a valid path for the command
  static void return_valid_path(String[] arr, String[] env) {
    for (String s : env) {
      File tmp = new File(s + "/" + arr[0]);
      if (tmp.exists()) {
        arr[0] = s + "/" + arr[0];
        break;
      }
    }
  }

  // Method to split the input into pipes, commands, and parameters
  static String[][][] splitInput() {
    Scanner in = new Scanner(System.in);
    // Prompting for input
    System.out.print("shell> ");
    String input = in.nextLine();

    // Separating the input into pipes
    String[] pip_sep = input.split(" \\| ");

    // Separating the pipes into commands
    String[][] sep_input = new String[pip_sep.length][];
    for (int i = 0; i < pip_sep.length; i++) {
      sep_input[i] = pip_sep[i].split(" && ");
    }

    // Separating the commands into parameters
    String[][][] split_input = new String[sep_input.length][][];
    for (int i = 0; i < sep_input.length; i++) {
      split_input[i] = new String[sep_input[i].length][];
      for (int j = 0; j < sep_input[i].length; j++) {
        split_input[i][j] = sep_input[i][j].split(" ");
      }
    }

    // Printing the split input for debugging purposes
    for (String[][] d2 : split_input) {
      for (String[] d1 : d2) {
        System.err.println(Arrays.toString(d1));
      }
    }

    return split_input;
  }

  static int create_proc(
    String[] split_input,
    String fin,
    String fout,
    int[] pip_fd,
    int new_stdin,
    int new_stdout
  ) {
    // check if the command is 'exit'
    if (split_input[0].equals("exit")) {
      System.err.println("\n");
      exit(0);
    }

    int[] status = new int[1];
    int pid_child;

    // create a new process
    if ((pid_child = fork()) < 0) {
      System.err.println("Error: fork");
      exit(1);
    }

    // distinguish between child and parent processes
    if (pid_child == 0) {
      // child process

      // read from pipe
      if (new_stdin != STDIN_FILENO && new_stdout != STDOUT_FILENO) {
        close(new_stdout);
        dup2(new_stdin, STDIN_FILENO);
        close(new_stdin);
      }

      // redirection with '<' or '>'
      if (fout != null || fin != null) {
        if (redirect_output(fout, fin) != 0) {
          System.err.println("ERROR: Error redirecting output");
          return 1;
        }
      }

      // write to pipe
      if (pip_fd[0] != STDIN_FILENO && pip_fd[1] != STDOUT_FILENO) {
        close(pip_fd[0]);
        dup2(pip_fd[1], STDOUT_FILENO);
        close(pip_fd[1]);
      }

      // execute the command
      if (execv(split_input[0], split_input) < 0) {
        System.err.println("ERROR: execv");
        exit(1);
      }
    } else {
      // parent process

      // close the file descriptors for the pipe
      if (new_stdin != STDIN_FILENO && new_stdout != STDOUT_FILENO) {
        close(new_stdin);
        close(new_stdout);
      }

      // wait for the child process to finish
      if (waitpid(pid_child, status, 0) < 0) {
        System.err.println("ERROR: waiting for child");
        exit(1);
      }

      // check the exit status of the child process
      if (status[0] != 0) {
        System.err.println("LOG: Child returned error code");
        return 1;
      }
    }

    return 0;
  }
}
