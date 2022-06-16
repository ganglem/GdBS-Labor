import static cTools.KernelWrapper.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

class hello{

	final static int fd_in = 0;
	final static int fd_out = 1;


	public static void main(String[] args) throws Exception {

		while(true) {

			System.out.print("shell>");
			BufferedReader buffRead = new BufferedReader(new InputStreamReader(System.in));
			String[] chain = buffRead.readLine().split("\\|\\s+");
			// \\| splits if there's a pipe
			// \\s+ split if there are one or multiple spaces


			// for piping
			int in = 0;
			int[] pipefd = new int[2];


			// loops through input
			for(int j = 0; j < chain.length; j++){


				boolean error = false;

				String[] input = chain[j].split("\\s+");


				// exit prompt
				if(chain[0].equals("exit")) {
					System.out.print("exiting...");
					exit(0);
				}


				// umlenken
				boolean stdinUmlenken = false;
				boolean stdoutUmlenken = false;
				int stdinOld = 0;
				int stdoutOld = 1;

				// stdin umlenken
				for (int i = 0; i<input.length; i++) {

					if(input[i].equals("<")) {

						stdinUmlenken = true;
						String path2 = input[i+1];
						stdinOld = dup2(fd_in, 100);

						close (fd_in);
						open(path2, O_RDONLY);

						String[] buffer = new String[input.length-2];
						for (int k = 0; k<buffer.length; k++) {
							buffer[k] = input[k];
						}
						input = buffer;
						break;
					}

				}// end stdin umlenken

				// stdout umlenken
				String path3 = null;
				for (int i = 0; i<input.length; i++) {

					if(input[i].equals(">")) {

						stdoutUmlenken = true;
						path3 = input[i+1];
						stdoutOld = dup2(fd_out, 101);

						close(fd_out);
						open(path3, O_WRONLY | O_CREAT);

						String[] buffer2 = new String[input.length-2];
						for (int k = 0; k<buffer2.length; k++) {
							buffer2[k] = input[k];
						}
						input = buffer2;

					}

				}// end stdout umlenken



				//Aufgabe 2 Kommandoverkettung



				// reset
				if(stdinUmlenken == true) {
					close(fd_in);
					dup2(stdinOld, fd_in);
					close(stdinOld);
				}
				if(stdoutUmlenken == true) {
					close(fd_out);
					dup2(stdoutOld, fd_out);
					close(stdoutOld);
				}
				if(error) {
					break;
				}

			}// end for

		}// end while

	}//end main


}//end class

