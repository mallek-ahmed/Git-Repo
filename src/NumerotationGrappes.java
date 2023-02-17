import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class NumerotationGrappes {

    public static void main(String[] args) throws IOException {
        String inputFile = "input.txt"; // chemin du fichier d'entrée
        String outputFile = "output.txt"; // chemin du fichier de sortie

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String currentKey = "";
            int currentGroup = 0;

            String line;
            while ((line = reader.readLine()) != null) {
                String key = line.substring(0, 27);
                String recordType = line.substring(27, 29);
                String debutLine = line.substring(0, 39);
                String resteLine = line.substring(41);

                    if (currentKey.equals(key)) {
                        if (recordType.equals("01")) {
                            // On commence un nouveau groupe
							currentGroup++;
                            String groupNumber = String.format("%02d", currentGroup);
                            String newLine = debutLine + groupNumber + resteLine;
                            writer.write(newLine);
                            writer.newLine();
                        }
						else {
                            // les autres enregistrements à la fin de la grappe ont le même numéro que la fin de groupe
                            String groupNumber = String.format("%02d", currentGroup);
                            String newLine = debutLine + groupNumber + resteLine;
                            writer.write(newLine);
                            writer.newLine();
                        }
                    } else {
                        // nous avons atteint un nouveau groupe
                        currentKey = key;
                        currentGroup = 1;
                        String groupNumber =  String.format("%02d", currentGroup);
                        String newLine = debutLine + groupNumber + resteLine;
                        writer.write(newLine);
                        writer.newLine();
                    }
            }
        }
    }
}
