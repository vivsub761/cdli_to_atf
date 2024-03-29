/*
    nuolenna.java converts transliterated cuneiform text into cuneiform
    Copyright (C) 2018 Tommi Jauhiainen

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.util.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class nuolenna {

	private static BufferedWriter writer = null;
	
	private static TreeMap<String,String> cuneiMap = new TreeMap<String,String>();

	public static void main(String[] args) {
		
		File file = new File("sign_list.txt");
		
		loadindictionary(file);

		File file2 = new File(args[0]);
		
		muutanuoliksi(file2);
	}
	
	private static void muutanuoliksi(File file) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			
			String line = "";
			while ((line = reader.readLine()) != null) {
//				Print out the line with the TEXT ID
				line = line.trim();
				if (line.matches("&P\\d{6}.*")) {
					System.out.println(line);
					continue;
				}
//				Print out following lines
//				@ = indicates position of text on tablet, i.e "@column", "@reverse"
// 				$ = gives useful information that we want to print, i.e "$ beginning broken"
// 				# = when at beginning of line, indicates language, i.e #atf: lang sux
// 				>> = corresponds to a seal that reflects a name on the tablet

				if (line.startsWith("@") || line.startsWith("$") || line.startsWith("#") || line.startsWith(">>")
						|| line.startsWith("||")) {
					System.out.println(line);
					continue;
				}
//				Print line number
				if (line.matches("\\d+.*\\..*")) {

					System.out.print(line.split(" ")[0]);
					line = line.replaceAll("^[\\S]+", "");
				}

//	several modifications are made here in order to split the line into smaller more manageable chunks. All normal
//	parentheses are removed, as well as dashes and plus signs.
				line = line.replaceAll("[X×]", "x");
				line = line.replace("(", "");
				line = line.replace(")", "");
				line = line.replace("-", " ");
				line = line.replace("{+", "{");
				line = line.replace("+", " ");
				line = line.toLowerCase().trim();
//There are some issues in cases like this: {...}... where the transliteration is being treated as one instead of two separate
// transliterations. This remedies that by adding a space in between e.g {da}ta -> {da} ta
				for (String sa : line.split(" ")) {
					if (!cuneiMap.containsKey(sa.replaceAll("[\\!\\#\\?\\[\\{\\\\}\\]\\>\\<]", ""))) {
						line = line.replaceAll("([\\w\\!\\?\\#\\}\\]\\)\\>]+)([\\[\\{\\<\\(])", "$1 $2");
						line = line.replaceAll("([\\]\\}\\>\\!\\?\\#\\|])([\\w\\)]+)", "$1 $2");
					}
				}
// Logograms are written in capitals, but the signs are the same
				String[] sanat = line.split(" ");

				for (String sana : sanat) {
// REPETITION '(' GRAPHEME ')'
//					Made some modifications here, added the "&& !cuneiMap.containsKey(sana)" to check
//					if the number is in the sign list as it is. Also accounted for possible square or curly
//					brackets, i.e [1(n01] so that they can be printed in brackets.
					sana = sana.replace("_", "");
					sana = sana.replace("'", " ");
					sana = sana.replace("|", "");

					sana = sana.toLowerCase().trim();

//	Strips sana of all special characters and parentheses to check if there is a key for it in the hashmap.
					String check = sana.replaceAll("[\\[\\{\\<\\]\\}\\>\\!\\?\\#]", "");
//If sana is a compound character, get the first part of it
					String checker = check.replaceAll("[\\.x].*", "");
//	This piece of code handles repetition characters. For example 10(disz) repeats disz 10 times.
					if (((sana.matches("^[1-90]+\\(.*\\)[\\!\\#\\?]*$") && !cuneiMap.containsKey(sana)) ||
							sana.matches("^[\\[\\{\\<]*[1-90]+.*[\\]\\}\\>\\!\\?\\#]*$")) && !cuneiMap.containsKey(check) && !cuneiMap.containsKey(checker)) {
						Pattern checkParentheses = Pattern.compile("\\A([\\{\\[\\< ]*).*?([\\}\\]\\> \\!\\?\\#]*)\\z");
						Matcher match = checkParentheses.matcher(sana);
						if (match.find()) {
							String left = match.group(1);
							String right = match.group(2);
							sana = sana.replace(left, "");
							sana = sana.replace(right, "");
							String ending = handleChar(sana);
							if (!ending.isEmpty()) {
								sana = sana.replace(ending, "");
							}
							String merkki = sana.replaceAll("^[1-90]+", "");
							int maara = Integer.valueOf(sana.replaceAll("[^\\d]+.*$", ""));
							sana = merkki;
							while (maara > 1) {
								sana = sana + " " + merkki;
								maara = maara - 1;
							}
							sana = left + sana + ending + right;
						}
					}
// $-sign means that the reading is uncertain (the sign is still certain) so we just remove all dollar signs
					sana = sana.replaceAll("[\\$]", "");
// some complicated combination characters have their own sign in UTF, transformations here before removing pipes
//					sana = sana.replaceAll("gad\\&gad\\.gar\\&gar", "kinda");
					sana = sana.replaceAll("bu\\&bu\\.ab", "sirsir");
					sana = sana.replaceAll("tur\\&tur\\.za\\&za", "zizna");
					sana = sana.replaceAll("še\\&še\\.tab\\&tab.gar\\&gar", "garadin3");

// LAGAŠ = ŠIR.BUR.LA
					sana = sana.replaceAll("lagasz ", "šir bur la ");

					String[] tavut = sana.split(" ");

					for (String tavu : tavut) {
						String checks = tavu.replaceAll("[\\!\\?\\#\\{\\[\\<\\}\\]\\>]", "");
						boolean contain = cuneiMap.containsKey(checks);
						tavu = tavu.toLowerCase().trim();
// After the characters @ and ~ there is some annotation which should not affect cuneifying, so we just remove it.
// Before doing this, we check if there are any significant symbols at the end of the character(eg !,#,?)
						if (tavu.matches(".*@[19cghknrstvz].*") && !contain) {
							String ending = handleChar(tavu);
							tavu = tavu.replaceAll("@.*", "");
							if (!ending.isEmpty()) {
								tavu = tavu + ending;
							}
						}

						if (tavu.matches(".*~[abcdefptyv][1234dgpt]?p?.*")) {
							tavu = tavu.replaceAll("~[abcdefptyv][1234dgpt]?p?", "");
						}
// All numbers to one
						if (tavu.matches("[n][1-90][1-90]*~*.*") && !contain) {
							Pattern checkParentheses = Pattern.compile("\\A([\\{\\[\\<\\( ]*).*?([\\}\\]\\>\\) ]*[\\!\\?\\#]*)\\z");
							Matcher match = checkParentheses.matcher(tavu);
							tavu = "n01";
							if (match.find()) {
								parentheses(match.group(1), match.group(2), tavu);
								continue;
							}
						}
						if (tavu.matches("^[m][1-90][1-90]*~*.*$")) {
							Pattern checkParentheses = Pattern.compile("\\A([\\{\\[\\<\\( ]*).*?([\\}\\]\\>\\) ]*[\\!\\?\\#]*)\\z");
							Matcher match = checkParentheses.matcher(tavu);
							tavu = "m";
							if (match.find()) {
								parentheses(match.group(1), match.group(2), tavu);
								continue;
							}
						}
						
						if (tavu.equals("1/2(iku)") || tavu.equals("1/4(iku)")) {
							tavu = "";
						}

//	If tavu has an x or a ., it is a compound character that needs to be split up. This chunk of code handles that.
						if ((tavu.contains("x") || tavu.contains(".")) && !tavu.contains("&") && !checks.equals("...") && !contain) {
							tavu = tavu.replaceAll("[\\.]", "x");
							String[] alatavut = tavu.split("x");
							for (String alatavu: alatavut) {
								Pattern checkParentheses = Pattern.compile("\\A([\\{\\[\\<\\( ]*).*?([\\}\\]\\>\\) ]*[\\!\\?\\#]*)\\z");
								Matcher match = checkParentheses.matcher(alatavu);
								String char1 = "";
								String char2 = "";
								if (match.find()) {
									char1 = match.group(1);
									char2 = match.group(2);
								}
								alatavu = alatavu.replace(char1, "");
								alatavu = alatavu.replace(char2, "");
								if (cuneiMap.containsKey(alatavu)) {
									System.out.print(char1 + cuneiMap.get(alatavu) + char2);
								}
							}
						}

//	Checks for parentheses around the transliteration that need to be preserved.
						else if (tavu.matches("\\A[\\{\\[\\<\\( ]+.*") || tavu.matches(".*[ \\}\\]\\>\\)]+[\\!\\?\\#]*\\z") && !cuneiMap.containsKey(tavu)) {
							Pattern checkParentheses = Pattern.compile("\\A([\\{\\[\\<\\( ]*).*?([\\}\\]\\>\\) ]*[\\!\\?\\#]*)\\z");
							Matcher match = checkParentheses.matcher(tavu);
							if (match.find()) {
								parentheses(match.group(1), match.group(2), tavu);
								continue;
							}

						}
						// Checks if there is an ending consisting of the characters #!?.
						String ending = handleChar(tavu);
						if (!ending.isEmpty()) {
							for (String symbol : ending.split("")) {
								tavu = tavu.replace(symbol, "");
							}
						}
						if (cuneiMap.containsKey(tavu)) {
							System.out.print(cuneiMap.get(tavu) + ending);
						}
						else if (tavu.equals("€")) {
							System.out.print("  ");
						}
					}
				}
				System.out.print("\n");
			}
		reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void loadindictionary(File file) {
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			
			String line = "";
			
			while ((line = reader.readLine()) != null) {
				String translitteraatio = line.replaceAll("\t.*", "");
				translitteraatio = translitteraatio.toLowerCase();
				String nuolenpaa = line.replaceAll(".*\t", "");
// We'll change all combination signs to just signs following each other
				nuolenpaa = nuolenpaa.replaceAll("[xX×]", "x");

// We make substitutions to conform to our desired format(convert subscripts to integers, replace accents)
				translitteraatio = translitteraatio.replace("|", "");
				translitteraatio = translitteraatio.replace("š", "sz");
				translitteraatio = translitteraatio.replace("×", "x");
				translitteraatio = translitteraatio.replace("X", "x");
				translitteraatio = translitteraatio.replace("ṭ", "t,");
				translitteraatio = translitteraatio.replace("ₓ", "x");
				translitteraatio = translitteraatio.replace("ṣ", "s,");
				translitteraatio = translitteraatio.replace("ŋ", "j");
				translitteraatio = translitteraatio.replace("Ḫ", "H,");
				translitteraatio = translitteraatio.replace("ₓ", "");
				translitteraatio = translitteraatio.replace("ḫ", "h,");
				translitteraatio = translitteraatio.replace("₁", "1");
				translitteraatio = translitteraatio.replace("₂", "2");
				translitteraatio = translitteraatio.replace("₃", "3");
				translitteraatio = translitteraatio.replace("₄", "4");
				translitteraatio = translitteraatio.replace("₅", "5");
				translitteraatio = translitteraatio.replace("₆", "6");
				translitteraatio = translitteraatio.replace("₇", "7");
				translitteraatio = translitteraatio.replace("₈", "8");
				translitteraatio = translitteraatio.replace("₉", "9");
				translitteraatio = translitteraatio.replace("ś", "s'");
				translitteraatio = translitteraatio.replace("Ś", "s'");
				translitteraatio = translitteraatio.replace("ʾ", "");
				translitteraatio = translitteraatio.replace("'", " ");
				translitteraatio = translitteraatio.replaceAll("[\\(\\)]", "");

// we add to cuneimap only if there is a transliteration

				if (translitteraatio.length() > 0) {
					cuneiMap.put(translitteraatio, nuolenpaa);
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	Checks if the symbol contains any parentheses around it. If it does, then print the symbol
//	with the appropriate parentheses.
	private static void parentheses(String char1, String char2, String tavu) {

		tavu = tavu.replace(char1, "");
		tavu = tavu.replace(char2, "");
		String ending = handleChar(tavu);
		if (!ending.isEmpty()) {
			tavu = tavu.replace(ending, "");
		}
		if (cuneiMap.containsKey(tavu)) {
			System.out.print(char1 + cuneiMap.get(tavu) + ending + char2);
		}
		if (tavu.equals("...")) {
			System.out.print(char1 + tavu + ending + char2);
		}
	}
//	This function checks if the string passed in has any important characters at the end
//	such as !, ?, and # and returns a string containing all of these characters if it does
	private static String handleChar(String tavu) {
		String ending = "";
		Pattern symbolCheck = Pattern.compile("([\\!\\#\\?]*)\\z");
		Matcher match = symbolCheck.matcher(tavu);
		if (match.find()) {
			ending = match.group(0);
		}
		return ending;
	}
}
