/* Rob Wynne, MSC
 *
 * Create a Changes report for an
 * old and new CDISC report
 */


package gov.nih.nci.evs.cdisc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Vector;

public class Diff {

	public HashMap<Codelist, ArrayList<Element>> newMap = new HashMap<Codelist, ArrayList<Element>>();
	public HashMap<Codelist, ArrayList<Element>> oldMap = new HashMap<Codelist, ArrayList<Element>>();
	public String releaseDate;
	public String requestCode = "";
	// public String changeType = "unknown";
	public Vector<Change> changes = new Vector<Change>();
	public PrintWriter pw;

	/*
	 * 0 - new report filename
	 * 1 - old report filename
	 * 2 - release date
	 * 3 - outputfile
	 */
	public static void main(String args[]) {
		Diff report = new Diff();
		if( args.length == 4 ) {
			System.out.println("Initializing diff report...");
			report.init(args[0], args[1], args[2], args[3]);
			System.out.println("Getting changes...");
			report.getChanges();
			System.out.println("Printing changes report...");
			report.print();
		}
		else {
			printHelp();
		}
	}

	public static void printHelp() {
		//TODO: Create batch scripts
		System.out.println("Unable to start program due to improper input parameters. Program will exit.");
		System.out.println();
		System.out.println("Run as: DiffCDISC [new report text file] [previous report text file] [release date] [output filename]");
		System.out.println("Example: DiffCDISC \"SDTM Terminology.txt\" \"SDTM Terminology 2014-03-28.txt\" \"2014-06-27\" \"D:\\data\\SDTM Changes.txt\"");
		System.out.println();
		System.exit(0);
	}

	private void config_pw(String fileLoc) {
		try {
			File file = new File(fileLoc);
			pw = new PrintWriter(file);
		} catch (Exception e) {
			System.out.println("Error in PrintWriter");
		}
	}

	public void init(String newReport, String oldReport, String date, String saveDestination) {
		newMap = parseCDISC(newReport);
		oldMap = parseCDISC(oldReport);
		releaseDate = date;
		config_pw(saveDestination);
	}

	public void getChanges() {

		for(Codelist cl_new : newMap.keySet()) {
			boolean found = false;
			for(Codelist cl_old : oldMap.keySet() ) {
				if( cl_new.getCode().equals(cl_old.getCode()) ) {
					found = true;
					compareCodelists(cl_new, cl_old);
					if( newMap.get(cl_new) != null && oldMap.get(cl_old) != null )
						compareElements(newMap.get(cl_new), oldMap.get(cl_old), cl_new.getSubValue());
					break;
				}
			}
			if( !found ) {
				getChangesForNewCodelist(cl_new, newMap.get(cl_new));
			}
		}

		for(Codelist cl_old : oldMap.keySet()) {
			boolean found = false;
			for(Codelist cl_new : newMap.keySet()) {
				if(cl_old.getCode().equals(cl_new.getCode()) ) {
					found = true;
				}
			}
			if( !found ) {
				getChangesForRemovedCodelist(cl_old, oldMap.get(cl_old));
			}
		}
	}

	public void getChangesForRemovedCodelist(Codelist oldCodelist, ArrayList<Element> oldElements) {
		changes.add(new Change(releaseDate, requestCode, "Remove", oldCodelist.getCode(), "CDISC Codelist", oldCodelist.getSubValue(), oldCodelist.getName(), "Retire codelist", oldCodelist.getSubValue(), "- - -"));
		if( oldElements != null ) {
			for(Element e: oldElements) {
				changes.add(new Change(releaseDate, requestCode, "Remove", e.getCode(), "Term", oldCodelist.getSubValue(), e.getName(), "Remove term from retired codelist", e.getSubValue(), "- - -"));
			}
		}
		else {
			System.out.println("No Terms associated with codelist " + oldCodelist.getName());
		}
	}

	public void getChangesForNewCodelist(Codelist newCodelist , ArrayList<Element> newElements) {
		changes.add(new Change(releaseDate, requestCode, "Add", newCodelist.getCode(), "CDISC Codelist", newCodelist.getSubValue(), newCodelist.getName(), "Addition of new codelist", "- - -", newCodelist.getSubValue()));
		if( newElements != null ) {
			for(Element e : newElements ) {
				changes.add(new Change(releaseDate, requestCode, "Add", e.getCode(), "Term", newCodelist.getSubValue(), e.getName(), "Add new term to new codelist", "- - -", e.getSubValue() ));
			}
		}
		else {
			System.out.println("No Terms associated with new codelist "  + newCodelist.getName());
		}
	}

	public void compareElements(ArrayList<Element> els_new, ArrayList<Element> els_old, String clShortName ) {
		TreeMap<String, Element> newElements = new TreeMap<String, Element>();
		TreeMap<String, Element> oldElements = new TreeMap<String, Element>();

		for(Element el_new : els_new ) {
			newElements.put(el_new.getCode(), el_new);
		}

		for(Element el_old : els_old ) {
			oldElements.put(el_old.getCode(), el_old);
		}

		for(String newCode : newElements.keySet()) {
			boolean found = false;
			for(String oldCode : oldElements.keySet()) {
				if( newCode.equals(oldCode) ) {
					found = true;
					diffElements(newElements.get(newCode), oldElements.get(oldCode), clShortName);
				}
			}
			if( !found ) {
				getChangesForAddingTermToExistingCodelist(newElements.get(newCode), clShortName);
			}
		}

		for(String oldCode : oldElements.keySet()) {
			boolean found = false;
			for(String newCode : newElements.keySet()) {
				if(oldCode.equals(newCode)) {
					found = true;
					//no need to diff
				}
			}
			if( !found ) {
				getChangesForRemoveTermFromExistingCodelist(oldElements.get(oldCode), clShortName);
			}
		}

	}

	public void getChangesForRemoveTermFromExistingCodelist(Element e, String clShortName) {
		changes.add(new Change(releaseDate, requestCode, "Remove", e.getCode(), "Term", clShortName, e.getName(), "Remove term entirely from codelist", e.getSubValue(), "- - -"));
	}

	public void getChangesForAddingTermToExistingCodelist(Element e, String clShortName) {
		changes.add(new Change(releaseDate, requestCode, "Add", e.getCode(), "Term", clShortName, e.getName(), "Add new term to existing codelist", "- - -", e.getSubValue()));
	}

	public void diffElements( Element newElement, Element oldElement, String clShortName ) {
		if( !newElement.getName().equals(oldElement.getName()) )
			changes.add(new Change(releaseDate, requestCode, "Update", newElement.getCode(), "CDISC Codelist Name", clShortName, newElement.getName(), "Update CDISC Codelist Name", oldElement.getName(), newElement.getName()));
		if( !newElement.getSubValue().equals(oldElement.getSubValue()) )
			changes.add(new Change(releaseDate, requestCode, "Update", newElement.getCode(), "CDISC Submission Value", clShortName, newElement.getName(), "Update CDISC Submission Value", oldElement.getSubValue(), newElement.getSubValue()));
		if( !newElement.getDefinition().equals(oldElement.getDefinition()) )
			changes.add(new Change(releaseDate, requestCode, "Update", newElement.getCode(), "CDISC Definition", clShortName, newElement.getName(), "Update CDISC Definition", oldElement.getDefinition(), newElement.getDefinition()));
		if( !newElement.getPT().equals(oldElement.getPT()) )
			changes.add(new Change(releaseDate, requestCode, "Update", newElement.getCode(), "NCI Preferred Term", clShortName, newElement.getName(), "Update NCI Preferred Term", oldElement.getPT(), newElement.getPT()));
		String[] old_syns = oldElement.getSynonyms();
		String[] new_syns = newElement.getSynonyms();
		Arrays.sort(old_syns);
		Arrays.sort(new_syns);

		//forwards
		for(int i=0; i < new_syns.length; i++) {
			boolean found = false;
			for(int j=0; j < old_syns.length; j++) {
				if( new_syns[i].equals(old_syns[j])) {
					found = true;
				}
			}
			if( !found ) {
				if( !new_syns[i].equals("") ) {
					changes.add(new Change(releaseDate, requestCode, "Update", newElement.getCode(), "CDISC Synonym", clShortName, newElement.getName(), "Add new CDISC Synonym", "- - -", new_syns[i]));
				}
			}
		}

		//and backwards
		for(int i=0; i < old_syns.length; i++) {
			boolean found = false;
			for(int j=0; j < new_syns.length; j++) {
				if( old_syns[i].equals(new_syns[j])) {
					found = true;
				}
			}
			if( !found ) {
				if( !old_syns[i].equals("") ) {
					changes.add(new Change(releaseDate, requestCode, "Update", newElement.getCode(), "CDISC Synonym", clShortName, newElement.getName(), "Remove CDISC Synonym", old_syns[i], "- - -"));
				}
			}
		}

	}

	public void compareCodelists(Codelist cl_new, Codelist cl_old) {
		if( !cl_new.getExtensible().equals(cl_old.getExtensible()) )
			changes.add(new Change(releaseDate, requestCode, "Update", cl_new.getCode(), "CDISC Extensible List", cl_new.getSubValue(), cl_new.getName(), "Update CDISC Extensible List", cl_old.getExtensible(), cl_new.getExtensible()));
		if( !cl_new.getName().equals(cl_old.getName()) )
			changes.add(new Change(releaseDate, requestCode, "Update", cl_new.getCode(), "CDISC Codelist Name", cl_new.getSubValue(), cl_new.getName(), "Update CDISC Codelist Name", cl_old.getName(), cl_new.getName()));
		if( !cl_new.getSubValue().equals(cl_old.getSubValue()) )
			changes.add(new Change(releaseDate, requestCode, "Update", cl_new.getCode(), "CDISC Submission Value", cl_new.getSubValue(), cl_new.getName(), "Update CDISC Submission Value", cl_old.getSubValue(), cl_new.getSubValue()));
		if( !cl_new.getDefinition().equals(cl_old.getDefinition()) )
			changes.add(new Change(releaseDate, requestCode, "Update", cl_new.getCode(), "CDISC Definition", cl_new.getSubValue(), cl_new.getName(), "Update CDISC Definition", cl_old.getDefinition(), cl_new.getDefinition()));
		if( !cl_new.getPT().equals(cl_old.getPT()) )
			changes.add(new Change(releaseDate, requestCode, "Update", cl_new.getCode(), "NCI Preferred Term", cl_new.getSubValue(), cl_new.getName(), "Update NCI Preferred Term", cl_old.getPT(), cl_new.getPT()));
		String[] old_syns = cl_old.getSynonyms();
		String[] new_syns = cl_new.getSynonyms();
		Arrays.sort(old_syns);
		Arrays.sort(new_syns);

		//forwards
		for(int i=0; i < new_syns.length; i++) {
			boolean found = false;
			for(int j=0; j < old_syns.length; j++) {
				if( new_syns[i].equals(old_syns[j])) {
					found = true;
				}
			}
			if( !found ) {
				changes.add(new Change(releaseDate, requestCode, "Update", cl_new.getCode(), "CDISC Synonym", cl_new.getSubValue(), cl_new.getName(), "Add new CDISC Synonym", "- - -", new_syns[i]));
			}
		}

		//and backwards
		for(int i=0; i < old_syns.length; i++) {
			boolean found = false;
			for(int j=0; j < new_syns.length; j++) {
				if( old_syns[i].equals(new_syns[j])) {
					found = true;
				}
			}
			if( !found ) {
				changes.add(new Change(releaseDate, requestCode, "Update", cl_new.getCode(), "CDISC Synonym", cl_new.getSubValue(), cl_new.getName(), "Remove CDISC Synonym", old_syns[i], "- - -"));
			}
		}
	}

	public HashMap<Codelist, ArrayList<Element>> parseCDISC(String filename) {
		HashMap<Codelist, ArrayList<Element>> map = new HashMap<Codelist, ArrayList<Element>>();

		try {
			Scanner input = new Scanner(new File(filename), "windows-1252");
			int lineNum = 0;
			while(input.hasNext()) {
				lineNum++;
				String line = input.nextLine().trim();
				String[] tokens = line.split("\t");

				if( tokens.length == 8 ) {
					/* 0 - Code
					 * 1 - Codelist Code
					 * 2 - Extensible (codelist only)
					 * 3 - Codelist Name
					 * 4 - Submission Value
					 * 5 - Synonyms
					 * 6 - Definition
					 * 7 - PT
					 */
					String code = tokens[0];
					String codelistCode = tokens[1];
					String extensible = tokens[2];
					String codelistName = tokens[3];
					String subValue = tokens[4];
					String synonyms = tokens[5];
					String definition = tokens[6];
					String pt = tokens[7];
					// KLO, 10062021
					//if( !extensible.equals("") ) {
					if( !code.equals("") && codelistCode.equals("")) {
						Codelist cl = new Codelist(code, extensible, codelistName, subValue, synonyms, definition, pt);
						map.put(cl, null);
					}
					else {
						Element e = new Element(code, codelistCode, codelistName, subValue, synonyms, definition, pt);
						boolean found = false;
						for(Codelist cl : map.keySet()) {
							if(cl.getCode().equals(e.getCodelistCode())) {
								found = true;
								ArrayList<Element> tmp = map.get(cl);
								if( tmp == null ) {
									tmp = new ArrayList<Element>();
									tmp.add(e);
									map.put(cl, tmp);
								}
								else {
									tmp.add(e);
									map.put(cl, tmp);
								}
								break; //seen enough
							}
						}
						if( !found ) {
							System.err.println("Unable to find the codelist " + e.getCodelistCode() + " for element "  + e.getCode());
						}
					}
				}
				else {
					System.err.println("Unable to read " + filename + ": line " + lineNum);
				}
			}
			input.close();
		} catch (FileNotFoundException e) {
			System.err.println("Unable to open and parse file " + filename);
			e.printStackTrace();
		}
		return map;
	}

	public void print() {

		Collections.sort(changes, new Comparator<Change>() {
			public int compare(final Change change1, final Change change2) {
			    return change1.getNoCCode() - change2.getNoCCode();
			}
		});

		Collections.sort(changes, new Comparator<Change>() {
			public int compare(final Change change1, final Change change2) {
			    return change1.getShortName().compareTo(change2.getShortName());
			}
		});

		pw.println("Release Date" + "\t" + "Request Code" + "\t" + "Change Type" + "\t" +
				"NCI Code" + "\t" + "CDISC Term Type" + "\t" + "CDISC Codelist (Short Name)" +
				"\t" + "CDISC Codelist (Long Name)" + "\t" + "Change Summary" + "\t" + "Original" + "\t" + "New" +
			  	"\t" + "Change Implementation Instructions");

		for(Change c : changes ) {
			pw.print(c.toString());
		}
		pw.close();
	}

}
