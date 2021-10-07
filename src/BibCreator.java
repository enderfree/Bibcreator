import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.IOException;

public class BibCreator {
	//Prints the message in the exception you passed it and close the program
	private static void printExceptionAndCloseProgram(Exception e) {
		System.out.println(e.getMessage());
		System.exit(0);
	}
	
	//Delete all files in the folder "Outputs"
	private static void cleanOutputFolder() {
		File outputFolder = new File("Outputs");
		String[] files = outputFolder.list();
		
		for(String file : files) {
			File fileToDelete = new File("Outputs/" + file);
			fileToDelete.delete();
		}
	}
	
	//Receives the fields necessary to make an IEEE article and returns an IEEE article made from those fields
	private static String createIEEE(String author, String journal, String title, String year, String volume, 
			String number, String pages, String month) {
		return author + " \"" + title + "\", " + journal + ", vol. " + volume + ", no. " + number + ", p. " + pages + ", " + month + " " + year + ".\n\n";
	}
	
	//Receives the fields necessary to make an ACM article and returns an ACM article made from those fields
	//Some changes might be necessary in author, if needed, this function will perform those changes.
	private static String createACM(int id, String author, String journal, String title, String year, String volume, 
			String number, String pages, String doi) {
		//Modify author if needed
		if (author.indexOf("and") != -1) {
			String[] authors = author.split("and");
			author = authors[0] + "et al";
		}
		
		//Return ACM article
		return "[" + id + "] " + author + ". " + year + ". " + title + ". " + journal + ". " + volume + ", " + number + " (" + year + "), " + pages + ". DOI:https://doi.org/" + doi + ".\n\n";
	}
	
	//Receives the fields necessary to make a NJ article and returns a NJ article made from those fields
	private static String createNJ(String author, String journal, String title, String year, String volume, String pages) {
		return author + ". " + title + ". " + journal + ". " + volume + ", " + pages + "(" + year + ").\n\n";
	}
	
	//Verifies that the data in the bib content you passed is valid and if it is, create the content of the files to be created
	//returns the content of the files to be created
	private static String[] processFilesForValidation(String bib) throws FileInvalidException {
		//The text that will be in each version of the file (I use += so they need to be initialized)
		String ieee = "";
		String acm = "";
		String nj = "";
		
		String[] articles = bib.split("@"); //Separates articles into different strings
		int acmId = 1; //ACM requires an id but not the one from the bib so here it is declared, it is incremented at every article and reset when we change file
		for(String article : articles) {
			String[] fields = {"ARTICLE" /*for id*/, "author", "journal", "title", "year", "volume", //here I initialize the fields to their labels
					"number", "pages", "keywords", "doi", "ISSN", "month"}; //Some of them are never used but I still isolated them in case we need them in the future
			
			int i = 0;
			Boolean allFieldsArePresent = true;
			for(String field : fields) {
				int iInAticle = article.indexOf(field); //position of the field in the article
				
				if(iInAticle == -1) { //if the field is not present (indexOf returns -1 when a field is not present)
					
					//Empty the fields so no data accidentally reaches somewhere before telling that not are field are present and stopping the operations on the fields of this article
					for(int j = 0; j < fields.length; ++j) {
						fields[j] = "";
					}
					allFieldsArePresent = false; 
					break; 
				}
				
				String separator = "}";
				if(i == 0) {
					separator = ","; //the id has a different separator
				}
				
				fields[i] = article.substring(iInAticle + field.length() + 2, article.indexOf(separator, iInAticle)); //substring(2char after the label to the position of the next } after the label)
				
				if(fields[i].length() == 0) {
					String fieldName = field=="ARTICLE"?"id":field; //I didn't have the choice to make it a variable, putting it to be concatenated in the middle of the string like I do with the variable was cutting the string
					throw new FileInvalidException("Error: Detected Empty Field!\n"
							+ "============================\n"
							+ "\n"
							+ "Problem detected with input file: FILE\n"
							+ "File is Invalid: Field \"" + fieldName + "\" is Empty. Prossessing stopped at this point. Other empty fields may be present as well!\n");
				}
				
				++i;
			}

			//if all field are present, create the content of the files
			if(allFieldsArePresent) {
				ieee += createIEEE(fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], fields[7], fields[11]);
				acm += createACM(acmId, fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], fields[7], fields[9]);
				nj += createNJ(fields[1], fields[2], fields[3], fields[4], fields[5], fields[7]);
				
				++acmId;
			}
		}
		return new String[]{ieee, acm, nj}; //there is no out parameters in java
	}
	
	//Verifies that the file path sent as a parameter is valid, if it is not throws a FileNotFoundException
	public static void verifyThatTheFileExistInOutputsFolder(String filePath) throws FileNotFoundException {
		File file = new File(filePath);
		if (!file.exists()) {
			System.out.println("This file is not present in the Outputs folder!");
			throw new FileNotFoundException();
		}
	}
	
	public static void main(String[] args) {
		int nbOfFiles = new File("Latex").list().length; //nb of files that will be attempted to convert
		int nbOfInvalidFiles = 0; //nb of files that failed to be converted so far
		for(int i = 1; i <= nbOfFiles; ++i) { //for each files
			//attempt to read the file content and send it in the String below
			String currentBibContent = "";
			
			try {
				File currentBib = new File("Latex/Latex" + i + ".bib");
				Scanner fileReader = new Scanner(currentBib);
				
				currentBibContent = "";
				while(fileReader.hasNextLine()) {
					currentBibContent += fileReader.nextLine();
				}
				
				fileReader.close();
			}
			catch(FileNotFoundException e) {
				FileInvalidException fie = new FileInvalidException("Could not open input file Latex" + i + ".bib for reading. \nPlease check if file exists in the Latex folder! Program will terminate after closing any opened files.");
				printExceptionAndCloseProgram(fie);
			}
			
			//Create the empty files with the right name
			String fileBeingCreated = ""; //I had no choice to declare it outside in order to pass it to a catch
			try {
				fileBeingCreated = "Outputs/IEEE" + i + ".json";
				PrintWriter pw = new PrintWriter(fileBeingCreated);
				pw.close();  
				fileBeingCreated = "Outputs/ACM" + i + ".json";
				pw = new PrintWriter(fileBeingCreated);
				pw.close(); //I am amazed that this does work in Java, why doesn't it in C#!
				fileBeingCreated = "Outputs/NJ" + i + ".json";
				pw = new PrintWriter(fileBeingCreated);
				pw.close(); 
			}
			catch(FileNotFoundException e) {
				FileInvalidException fie = new FileInvalidException("Could not create output file " + fileBeingCreated);
				cleanOutputFolder();
				printExceptionAndCloseProgram(fie);
			}
			catch(SecurityException e) {
				FileInvalidException fie = new FileInvalidException("The security manager and checkWrite() denies write access");
				cleanOutputFolder();
				printExceptionAndCloseProgram(fie);
			}
			
			//Write in the files I just created
			try {
				String[] files = processFilesForValidation(currentBibContent);
				
				File target = new File("Outputs/IEEE" + i + ".json");
				PrintWriter pw = new PrintWriter(target);
				pw.append(files[0]);
				pw.close();
				target = new File("Outputs/ACM" + i + ".json");
				pw = new PrintWriter(target);
				pw.append(files[1]);
				pw.close();
				target = new File("Outputs/NJ" + i + ".json");
				pw = new PrintWriter(target);
				pw.append(files[2]);
				pw.close();
			}
			catch(FileInvalidException e) {
				//Add the file name to the error message before printing it
				String message = e.getMessage().replace("FILE", "Latex/Latex" + i + ".bib");
				System.out.println(message);
				
				//Deletes the output files corresponding to the input file with faulty data
				File fileToDelete = new File("Outputs/IEEE" + i + ".json");
				fileToDelete.delete();
				fileToDelete = new File("Outputs/ACM" + i + ".json");
				fileToDelete.delete();
				fileToDelete = new File("Outputs/NJ" + i + ".json");
				fileToDelete.delete();
				
				++nbOfInvalidFiles;
			}
			catch(FileNotFoundException e) {
				//the file will be found, they were created just before this try catch
			}
		}
		
		System.out.println("A total of " + nbOfInvalidFiles + " files were invalid and could not be processed. All other " + (nbOfFiles - nbOfInvalidFiles) + " \"valid\" files have been created.\n\n"
				+ "Type the name of the file you would like to see: ");
		
		
		Scanner s = new Scanner(System.in);
		String filePath = "";
		for(int i = 0; i < 2; ++i) { //give a second chance but not more if the inputed file name is faulty
			try {
				filePath = "Outputs/" + s.nextLine();
				verifyThatTheFileExistInOutputsFolder(filePath);
				break; //if the file exist, you don't need the second chance
			}
			catch(FileNotFoundException e) {
				if (i == 1) { // 2 - 1 (1 less than the for upper bound) 
					System.out.println("Last chance exhausted: Closing the application");
					s.close(); //to avoid data leak despite the fact that I am closing the app just after
					System.exit(0);
				}
				else {
					System.out.println("Try again: ");
				}
			}
		}
		s.close();
		
		try {
			BufferedReader bf = Files.newBufferedReader(Paths.get(filePath), Charset.forName("ISO-8859-1")); //closing happens automatically when BufferedReader are created into a try
			String line; //I changed the Charset because some charaters in Latex10.bib weren't recognize by my default Charset, now it tolerates many new character with an accent
			
			//print the whole content of the file
			while((line = bf.readLine()) != null) { //to bad I can only assign like this, not declare
				System.out.println(line);
			}
		}
		catch(IOException e) {
			System.out.println("There was an error while opening the file");
		}
		catch(SecurityException e) {
			System.out.println("The security manager and checkWrite() denies write access");
		}
	}
}
