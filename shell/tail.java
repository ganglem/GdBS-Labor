import static cTools.KernelWrapper.*;
import java.util.Arrays;
import java.util.ArrayList;
//ToDo if FIlename = "-" read from stdin
class tail{

	public static void main(String[] args){
		int nbytes = 0;
		int nlines = 10;
        int start = 0;

		boolean param_used = false;

		//check if there are enough input args supplied
		if(args.length<1){
			System.out.println("Not enough args");
			exit(0);
		}

		if(args[0].charAt(0) == '-' && args[0].length()>1){
			switch(args[0].charAt(1)){
				case 'n':
                    nlines = Integer.parseInt(args[1]);
					if(nlines<0){System.err.println("ERROR: Input invalid.");exit(1);}
					param_used = true;
					break;
				case '-':
					if(args[0].contains("help")){
						help_text();
						exit(0);
					}break;	
			}
		}
		ArrayList<String> list = new ArrayList<>(Arrays.asList(args));

		if(param_used){
			//remove entries that arent files
			list.remove(0);
			list.remove(0);
		}
		
		//open one file at a time, read the specified amount and print it
		boolean footer = list.size()>1;
		for(String s : list){
			if(footer)
				System.out.println("FILE: " + s);
			int fd;
			if(s.equals("-")){
				fd = STDIN_FILENO;
			}else{
				if((fd = open(s,O_RDONLY))<0){
					System.err.println("Error opening File");
					exit(1);
				}
                
                start = get_start(fd, nlines);
                close(fd);
                if((fd = open(s,O_RDONLY))<0){
					System.err.println("Error opening File");
					exit(1);
				}
			}  
			if(nbytes == 0){
				//print lines
				read_and_println(fd,nlines,start);
			}else{
				//print bytes
				read_printbytes(fd,nbytes);
			}
			System.out.println();
		}
		exit(0);
		
	}

	private static int read_and_println(int fd,int nlines, int start){
		byte[] buffer = new byte[1];

		int res = 0;
		int len;
		int line_count = 0;
        int line_countER = 0;
		String out = "";

		while(true){
			if((res = read(fd,buffer,1))<=0) {
                break;
            }		
			if(buffer[0]=='\n'){
				line_count++;	
			}	
			//if(res == 0)
			//	break;
            if(line_count >= start) {
                out += (char)buffer[0];
            }
		}
		System.out.println(out);
		return 0;
	}

    private static int get_start(int fd, int nlines) {

        byte[] buffer = new byte[1];

		int res = 0;
		int len;
		int line_count = 0;
        int line_countER = 0;
		String out = "";

        //get line count
        while(true) {
            if((res = read(fd,buffer,1))<=0)
                break;
		
			if(buffer[0]=='\n'){
				line_countER++;	
			}	
        }

        int start = line_countER - nlines;

        return start;
    }

	private static int read_printbytes(int fd, int nbytes){
		byte[] buffer = new byte[256];

		int total = 0;
		int res;
		String out = "";

		while(total < nbytes){
			if((res=read(fd,buffer,nbytes-total))<0)
				return -1;
			total += res;
			if(res==0)break;
			out += new String(buffer);
		}
		System.out.println(out);

		return 0;
	}

    private static void help_text() {

        String help_text = 
        "Prints the last 10 lines of each file.\n\n -n +NUM\n    Print last NUM lines";

        System.out.println(help_text);
    }
}


