
//package shell;
import static cTools.KernelWrapper.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

class hello {

	final static int fd_in = 0;
	final static int fd_out = 1;

	public hello() {
	}

	public static void main(String[] args) throws Exception {
		while (true) {
			System.out.print("gib was ein: ");
			BufferedReader bufReader = new BufferedReader(new InputStreamReader(System.in));
			String[] kette = bufReader.readLine().split("\\|\\s+");

			int in = 0; // speichert vorübergehend fd0 für den nächsten Befehl
			int[] pipefd = new int[2];// array für pipe(); aus der Doku:
			// The array pipefd is used to return two
			// file descriptors referring to the ends of the pipe. pipefd[0]
			// refers to the read end of the pipe. pipefd[1] refers to the write
			// end of the pipe. Data written to the write end of the pipe is
			// buffered by the kernel until it is read from the read end of the
			// pipe.

			for (int j = 0; j < kette.length; j++) {
				String[] input = kette[j].split("\\s+");

				boolean error = false;

				if (kette[0].equals("exit")) {
					System.out.println("bye for now!");
					exit(0);
				}

				boolean stdinUmlenken = false;
				// standardmaessig muss nicht umgelenkt werden
				boolean stdoutUmlenken = false;
				int stdin_old = 0;
				int stdout_old = 1;

				// stdin umlenken
				for (int i = 0; i < input.length; i++) { //
					if (input[i].equals("<")) {
						stdinUmlenken = true;
						String path2 = input[i + 1];
						// File inFile = new File(path2);
						stdin_old = dup2(fd_in, 100); // backup origin stdin
						// doku:
						// int dup2(int oldfd, int newfd);
						// The dup2() system call performs the same task as
						// dup(), but instead of
						// using the lowest-numbered unused file descriptor, it
						// uses the descriptor number specified in newfd. If the
						// descriptor newfd was previously open, it is silently
						// closed before being reused.
						close(fd_in);
						open(path2, O_RDONLY);
						String[] buffer = new String[input.length - 2];
						for (int k = 0; k < buffer.length; k++) {
							buffer[k] = input[k];
						}
						input = buffer;
						break;
					}
				}
				// ende stdin umlenken

				String path3 = null;
				for (int i = 0; i < input.length; i++) {
					if (input[i].equals(">")) {
						stdoutUmlenken = true;
						path3 = input[i + 1];
						stdout_old = dup2(fd_out, 101); // back up original
														// stdout
						close(fd_out); // close original stdout
						open(path3, O_WRONLY | O_CREAT); // open or create new
															// stdout
						String[] buffer2 = new String[input.length - 2];
						for (int k = 0; k < buffer2.length; k++) {
							buffer2[k] = input[k];
						}
						input = buffer2;
					}
				}
				// Ende stdout umlenken

				// Aufgabe 2:
				String path = null;
				String PATH = System.getenv("PATH");
				PATH += ":./src";
				String[] pathArray = PATH.split(":");
				// die vom System vorgegebenen Pfade werden später nach der
				// auszuführenden Datei durchsucht
				File file;
				for (int i = 0; i < pathArray.length; i++) {
					file = new File(pathArray[i] + "/" + input[0]);
					// z.B. bin/nano
					if (file.isFile() && file.canExecute()) {
						// file ist tatsächlich eine Datei, außerdem ist diese
						// ausführbar
						path = pathArray[i] + "/" + input[0];
						break;
					}
				} // Ende Aufgabe 2;

				if (path == null) {
					System.err.println("Kein ausführbares Programm gefunden!");
					error = true;
				} else {
					if (j < kette.length - 1) { // also noch vor letztem
												// Durchgang
						pipe(pipefd);
						// pipe() creates a pipe, a unidirectional data channel
						// that can be used for interprocess communication. The
						// array pipefd is used to return two file descriptors
						// referring to the ends of the pipe. pipefd[0] refers
						// to the read end of the pipe. pipefd[1] refers to the
						// write end of the pipe. Data written to the write end
						// of the pipe is buffered by the kernel until it is
						// read from the read end of the pipe.
						error = execute(in, pipefd[1], path, input);
						// execute: siehe Methode unten;
						close(pipefd[1]);
						in = pipefd[0];
					} else { // letzter Druchgang
						int child_pid = fork();
						if (child_pid == 0) { // Kindprozess
							if (in != 0) {
								dup2(in, fd_in);
							}
							execv(path, input);
							// ersetzt den Kindprozess durch ein Programm,
							// welchem weitere
							// Parameter uebbergeben werden koennen
						} else if (child_pid == -1) {
							System.err.println("Starten der Anwendung fehlgeschlagen");
							error = true;
						} else { // Elternprozess
							int[] status = new int[1];
							waitpid(child_pid, status, 0);
						}

					}
				}

				// speichert ursprüngliche stdin / stdout
				if (stdinUmlenken == true) {
					close(fd_in);
					dup2(stdin_old, fd_in);
					close(stdin_old);
				}
				if (stdoutUmlenken == true) {
					close(fd_out);
					dup2(stdout_old, fd_out);
					close(stdout_old);
				}
				if (error) {
					break;
				}
			}
		}
	}

	private static boolean execute(int in, int out, String path, String[] input) {
		int child_pid = fork();
		if (child_pid == 0) { // Kindprozess
			if (in != fd_in) {
				dup2(in, fd_in);
				close(in);
			}
			if (out != fd_out) {
				dup2(out, fd_out);
				close(out);
			}
			execv(path, input);
			// ersetzt den Kindprozess durch ein Programm, welchem weitere
			// Parameter uebbergeben werden koennen
		} else if (child_pid == -1) { // Fehler
			System.err.println("Starten der Anwendung fehlgeschlagen");
			return true;
		}
		return false;
	}
}