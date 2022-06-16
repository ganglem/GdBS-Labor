import static cTools.KernelWrapper.O_RDONLY;
import static cTools.KernelWrapper.STDIN_FILENO;
import static cTools.KernelWrapper.close;
import static cTools.KernelWrapper.exit;
import static cTools.KernelWrapper.open;
import static cTools.KernelWrapper.read;

import java.util.ArrayList;
import java.util.Arrays;

class tail {

	public static void main(String[] args) {
		int number_bytes = 0;
		int number_lines = 10;	//default
		int starting_line = 0;

		boolean param_used = false;

		if (args.length < 1) {
			System.out.println("FAIL: Not enough args.");
			exit(0);
		}

		if (args[0].charAt(0) == '-' && args[0].length() > 1) {
			switch (args[0].charAt(1)) {
				case 'n':
					number_lines = Integer.parseInt(args[1]);
					if (number_lines < 0) {
						System.err.println("ERROR: Invalid input.");
						exit(1);
					}
					param_used = true;
					break;
				case '-':
					if (args[0].contains("help")) {
						help_text();
						exit(0);
					}
					break;
			}
		}
		ArrayList<String> file_list = new ArrayList<>(Arrays.asList(args));

		if (param_used) {
			// remove entries that aren't files
			file_list.remove(0);
			file_list.remove(0);
		}

		// open one file at a time, reads and prints it
		boolean more_than_one_file = file_list.size() > 1;
		for (String s : file_list) {
			if (more_than_one_file)
				System.out.println("CURRENT FILE: " + s + "\n");
			int fd;
			if (s.equals("-")) {
				// assign id
				fd = STDIN_FILENO;
			} else {
				if ((fd = open(s, O_RDONLY)) < 0) {
					System.err.println("ERROR: Error while opening File.");
					exit(1);
				}
				starting_line = get_start(fd, number_lines);
				// file needs to be closed and reopened
				// otherwise the file is already read til the end while determining line count
				close(fd);
				if ((fd = open(s, O_RDONLY)) < 0) {
					System.err.println("ERROR: Error while opening File.");
					exit(1);
				}
			}
			if (number_bytes == 0) {
				// print lines
				read_and_print_file(fd, number_lines, starting_line);
			} else {
				// print bytes
				read_printbytes(fd, number_bytes);
			}
			System.out.println();
		}
		exit(0);

	}

	/***
	 * Prints file text
	 * @param fd file id
	 * @param number_lines amount of lines to be printed
	 * @param starting_line
	 * @return exit code 0
	 */
	private static int read_and_print_file(int fd, int number_lines, int starting_line) {
		byte[] buffer = new byte[1];

		int res = 0;
		int line_count = 0;
		String out = "";

		while (true) {
			if ((res = read(fd, buffer, 1)) <= 0) {
				break;
			}
			if (buffer[0] == '\n') {
				line_count++;
			}
			// if(res == 0)
			// break;
			if (line_count >= starting_line) {
				out += (char) buffer[0];
			}
		}
		System.out.println(out);
		return 0;
	}

	/**
	 * Gets line number at which the program starts to print out the file
	 * @param fd file id
	 * @param number_lines amount of lines to be printed
	 * @return starting point
	 */
	private static int get_start(int fd, int number_lines) {

		byte[] buffer = new byte[1];

		int res = 0;
		int line_countER = 0;

		// get line count
		while (true) {
			if ((res = read(fd, buffer, 1)) <= 0)
				break;

			if (buffer[0] == '\n') {
				line_countER++;
			}
		}

		int starting_line = line_countER - number_lines;

		return starting_line;
	}


	//TODO
	//helper function to help with buffer allocation
	private static int read_printbytes(int fd, int number_bytes) {
		byte[] buffer = new byte[256];

		int total = 0;
		int res;
		String out = "";

		while (total < number_bytes) {
			if ((res = read(fd, buffer, number_bytes - total)) < 0)
				return -1;
			total += res;
			if (res == 0)
				break;
			out += new String(buffer);
		}
		System.out.println(out);

		return 0;
	}


	/***
	 * Prints help text when --help is passed
	 */
	private static void help_text() {

		String help_text = "Prints the last 10 lines of each file.\n\n -n +NUM\n    Print last NUM lines";

		System.out.println(help_text);
	}
}