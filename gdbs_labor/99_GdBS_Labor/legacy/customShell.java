import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static cTools.KernelWrapper.O_RDONLY;
import static cTools.KernelWrapper.O_WRONLY;
import static cTools.KernelWrapper.close;
import static cTools.KernelWrapper.dup2;
import static cTools.KernelWrapper.execv;
import static cTools.KernelWrapper.exit;
import static cTools.KernelWrapper.fork;
import static cTools.KernelWrapper.open;
import static cTools.KernelWrapper.pipe;
import static cTools.KernelWrapper.waitpid;

class customShell {
  public static void main(String[] args)
  {
    System.out.println("------ Console ------");
    System.out.println("waiting for input...");

    InputStream inStream = null;
    BufferedReader buffReader = null;
    try {
      inStream = System.in;
      buffReader = new BufferedReader(new InputStreamReader(inStream));
      String sLine = null;

      while ((sLine = buffReader.readLine()) != null) {
        String[] commands = sLine.trim().split("&&");

        for (int i = 0; i < commands.length; i++) {
          String commandToExec = commands[i].trim();
          String[] pipeSplit = commandToExec.split("\\|");
          if (pipeSplit.length > 1) {
            processPipedCommand(pipeSplit);
          }
          else {
            processCommand(commandToExec);
          }
          System.out.println("waiting for input...");
        }
      }
    }
    catch (IOException ioe) {
      System.out.println(ioe);
    }
    exit(0);
  }

  private static void closeAllPipes(ArrayList<int[]> pipeFds)
  {
    for (int[] fdTemp : pipeFds) {
      close(fdTemp[0]);
      close(fdTemp[1]);
    }
  }

  private static void processPipedCommand(String[] pipedCommandArr)
  {
    ArrayList<int[]> fds = new ArrayList<>(pipedCommandArr.length - 1);

    for (int i = 0; i < pipedCommandArr.length - 1; i++) {
      int[] fdTemp = new int[2];
      fds.add(i, fdTemp);

      if (pipe(fdTemp) == -1) {
        System.out.println("Error - pipe not established correctly.");
        return;
      }
    }

    int[] forkReturns = new int[pipedCommandArr.length];
    int pipeNr = -1;
    for (int i = 0; i < pipedCommandArr.length; i++) {
      String commandToExec = pipedCommandArr[i].trim();
      forkReturns[i] = fork();

      if (forkReturns[i] == 0) {
        // first child process
        if (i == 0) {
          pipeNr = i;
          dup2(fds.get(pipeNr)[1], 1);
          closeAllPipes(fds);
        }

        // last child process
        if (i == pipedCommandArr.length - 1) {
          pipeNr = pipedCommandArr.length - 2;
          dup2(fds.get(pipeNr)[0], 0);
          closeAllPipes(fds);
        }

        // child process between first and last
        if (i > 0 && i < pipedCommandArr.length - 1) {
          pipeNr = i;
          dup2(fds.get(pipeNr - 1)[0], 0);
          dup2(fds.get(pipeNr)[1], 1);
          closeAllPipes(fds);
        }

        // check for ">"
        String[] stdoutSplit = commandToExec.split(">");
        // check for "<"
        String[] stdinSplit = commandToExec.split("<");

        int fdToRedirect = -1;
        String command = "";
        if (stdoutSplit.length > 1) {
          fdToRedirect = redirectStreams(1, stdoutSplit);
          command = stdoutSplit[0].trim();
        }

        if (stdinSplit.length > 1) {
          fdToRedirect = redirectStreams(0, stdinSplit);
          command = stdinSplit[0].trim();
        }

        if (stdinSplit.length > 1 || stdoutSplit.length > 1) {
          String[] commandInput = command.split(" ");
          int error = execv("/bin/" + commandInput[0], commandInput);
          exit(error);

          if (fdToRedirect > -1) {
            close(fdToRedirect);
            //open("/dev/tty", O_RDWR);
          }
        }
        else {
          String[] commandInput = commandToExec.split(" ");
          int error = execv("/bin/" + commandInput[0], commandInput);
          exit(error);
        }
      }
    }

    closeAllPipes(fds);

    for (int i = 0; i < forkReturns.length; i++) {
      int[] status = new int[1];
      waitpid(forkReturns[i], status, 0);
    }
  }


  private static void processCommand(String commandToExec)
  {

    /*
     * TODO Lösch mich vor Abnahme.
     * Umlenk Fix.
     * Problem: Der auskommentierte Codeblock unten war die Lösung von Jule. Die hat allerdings
     * ZUERST den Stdin/StdOut umgelenkt und DANN geforked.
     * Bei Eingabe ls > test.txt wurde die damit die shell auch umgeleitet und man kann keinen neuen
     * Command ausführen. Der hier ausgeführte Code ist einfach von der Methode processPipeCommand rauskopiert, weil da die Reihenfolge
     * mit forken und umlenken richtig war. War zwar nicht 100% richtig, aber wurde abgenommen und funktioniert einigermaßen.
     */

    int forkReturns = fork();

    if (forkReturns != 0) {
      return;
    }

    // check for ">"
    String[] stdoutSplit = commandToExec.split(">");
    // check for "<"
    String[] stdinSplit = commandToExec.split("<");

    int fdToRedirect = -1;
    String command = "";
    if (stdoutSplit.length > 1) {
      fdToRedirect = redirectStreams(1, stdoutSplit); // fd 1 = stdout
      command = stdoutSplit[0].trim();
    }

    if (stdinSplit.length > 1) {
      fdToRedirect = redirectStreams(0, stdinSplit);
      command = stdinSplit[0].trim();
    }

    if (stdinSplit.length > 1 || stdoutSplit.length > 1) {
      String[] commandInput = command.split(" ");
//      int error = execv("/bin/" + commandInput[0], commandInput);
      int error = parseWildcardsAndRun(commandInput);
      exit(error);

      if (fdToRedirect > -1) {
        close(fdToRedirect);
      }
    }
    else {
      String[] commandInput = commandToExec.split(" ");
//      int error = execv("/bin/" + commandInput[0], commandInput);
      int error = parseWildcardsAndRun(commandInput);
      exit(error);
    }
  }


  private static int redirectStreams(int fdToRedir, String[] commandParts)
  {
    if (close(fdToRedir) == 0) {
      int fd = -1;
      if (fdToRedir == 0) {
        fd = open(commandParts[1].trim(), O_RDONLY);
      }
      else {
        fd = open(commandParts[1].trim(), O_WRONLY);
      }
      return fd;
    }
    return -1;
  }


  private static int parseWildcardsAndRun(String[] argv)
  {
    System.out.println("in parsewcandrun");
    for (String arg : argv) {
      if (arg.contains("*") || arg.contains("?")) {
        Path currentRelativePath = Paths.get("");
        String cwd = currentRelativePath.toAbsolutePath().toString();
        System.out.println("searching for " + arg + " in " + cwd);

        String[] expandedFiles = expandFiles(cwd, arg);
        if (expandedFiles.length == 0) {
          break;
        }
        argv = ArrayUtils.addAll(argv, expandedFiles);
      }
    }


    return execv("/bin/" + argv[0], argv);
  }

  private static String[] expandFiles(String path, String wildcardPattern)
  {
    File cwdDir = new File(path);
    FileFilter fileFilter = new WildcardFileFilter(wildcardPattern);

    File[] expandedFiles = cwdDir.listFiles(fileFilter);
    String[] results = {};

    assert expandedFiles != null;
    for (File file : expandedFiles) {
      System.out.println("found file " + file.getName());
      ArrayUtils.add(results, file.getAbsolutePath());
    }

    return results;
  }


  private static void issueUnixCommand(String command, String[] input)
  {
    // path extension
    String path = "/bin/" + command;

    /* TODO Lösch mich vor Abnahme.
     * Unsere Aufgabe 2 war es einen path zu hardcoden in dem (durch : getrennt) alle Ordner angegeben sind
     * in denen die shell ausgeführt werden soll. Diese Methode wurde nach dem "Umlenk fix" (siehe oben) allerdings nimmer aufgerufen.
     * Evtl zu fixen.
     */

    String paths = "/home/:../:./";
    String[] allPaths = paths.split(":");
    String[] execInput = new String[allPaths.length + 1];
    execInput[0] = input[0];
    if (allPaths.length > 1) {
      for (int i = 0; i < allPaths.length; i++) {
        execInput[i + 1] = allPaths[i];
      }
    }


    // forking
    int forkReturn = fork();
    if (forkReturn == 0) {
      int error = execv(path, execInput);
      exit(error);
    }
    else {
      int[] status = new int[1];
      waitpid(forkReturn, status, 0);
      if (status[0] != 0) {
        System.out.println("Error - exit with status: " + status[0]);
      }
    }

  }


}
