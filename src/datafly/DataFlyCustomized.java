/*
 * K-anonymization generalization algorithm as defined by Latanya Sweeney
 */
package datafly;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.sql.*;


/**
 * @author Valentin Langer
 * Code is based on Dunni Adenuga Datafly algorithm
 */
public class DataFlyCustomized {

    public static void main(String[] args) throws FileNotFoundException, ClassNotFoundException, SQLException {
        DataFlyCustomized dataFly = new DataFlyCustomized();

        PrivateTable myTestTable = dataFly.setupDataset();
        myTestTable.printFormat();
        myTestTable = dataFly.startGeneralization(myTestTable);


        System.out.println("Is the generated table 2-anonymous ? " + dataFly.checkTable(2, myTestTable));//this is just to check
        myTestTable.printFormat();

    }


    public boolean checkTable(int kanon, PrivateTable table){
        HashMap<ArrayList, Integer> freqSet = getFreqSet(table);
        Integer[] freqValues = new Integer[freqSet.size()];
        freqValues = freqSet.values().toArray(freqValues);
        for(int i = 0; i < freqValues.length; i++){
            if(freqValues[i] < kanon){
                return false;
            }
        }
        return true;
    }

    /**
     * Create a table and choose quasi identifiers
     * @return
     * @throws FileNotFoundException
     * @throws SQLException
     */
    public PrivateTable setup() throws FileNotFoundException, SQLException{
        /*Setting up*/
        PrivateTable myPrivateTable = new PrivateTable();
        //basically set attribute names
        myPrivateTable.setRowHeadings("Race,DOB,ID,Sex,Allele 1,Allele 2");

        /*instead of hard-code, in future should be user input*/
        myPrivateTable.setQuasi("Race,DOB,ID,Sex");// DO THIS LATER!!!

        myPrivateTable.setTableValues("/Users/valentinlanger/IdeaProjects/dataFly/src/datafly/tableInputs.txt");
        //myPrivateTable.setTableValues(conn);//uncomment this when connecting to DB
        myPrivateTable = rectifyTableColumn(myPrivateTable, 2);//rectify ID in this case

        //myPrivateTable.printFormat();

        return myPrivateTable;
    }

    public PrivateTable setupDataset() throws FileNotFoundException, SQLException{
        /*Setting up*/
        PrivateTable myPrivateTable = new PrivateTable();
        //basically set attribute names
        myPrivateTable.setRowHeadings("Age,Workclass,fnlwgt,Education,Education-num,Marital-Status,Occupation,Relatinship,Race,Sex,Capital-Gain,Captial-Loss,Hours-Per-Week,Native-Country,Salary");

        /*instead of hard-code, in future should be user input*/
        myPrivateTable.setQuasi("Age,Workclass");// DO THIS LATER!!!

        //myPrivateTable.setTableValues("/Users/valentinlanger/Library/Mobile Documents/com~apple~CloudDocs/Uni/6. Semester/BA/Datensatz/Adult Datensatz.txt");
        myPrivateTable.setTableValues("/Users/valentinlanger/Library/Mobile Documents/com~apple~CloudDocs/Uni/6. Semester/BA/Datensatz/Adult Test Datensatz.txt");

        myPrivateTable.printFormat();
        System.out.println(myPrivateTable.noOfColumns);

        myPrivateTable = rectifyTableColumn(myPrivateTable, 2);//rectify ID in this case

        //myPrivateTable.printFormat();

        return myPrivateTable;
    }

    /**
     * Generalize a table
     * @param myPrivateTable
     * @return
     * @throws FileNotFoundException
     */
    public PrivateTable startGeneralization(PrivateTable myPrivateTable) throws FileNotFoundException{
        System.out.print("Please Enter k: ");
        Scanner user = new Scanner(System.in);
        int kAnon = user.nextInt();
                /*a frequency list contains distinct sequences of values of PT[QI],
        along with the number of occurrences of each sequence.*/
        HashMap<Integer,Integer> columnsGeneralized = new HashMap<>();
        HashMap<ArrayList, Integer> freqSet = getFreqSet(myPrivateTable);
        ArrayList<DGHTreeCustomized> dghTrees = createDGHTrees(myPrivateTable);
        int generalizationLevel = 0;
        while(seqOccursLessThanKTimes(freqSet, kAnon)){

            int colToBeGeneralized = getAttributeWithMostDistinctValues(myPrivateTable, freqSet);//possibleColsToBeGeneralized[0];

            if(columnsGeneralized.containsKey(colToBeGeneralized)){
                columnsGeneralized.replace(colToBeGeneralized, columnsGeneralized.get(colToBeGeneralized),
                        columnsGeneralized.get(colToBeGeneralized)+1);
                generalizationLevel = columnsGeneralized.get(colToBeGeneralized);
                System.out.println("generation level: " + generalizationLevel);
            }
            else{
                columnsGeneralized.put(colToBeGeneralized, 1);
                generalizationLevel = 1;
                System.out.println("generation level: " + generalizationLevel);
            }
            //include DGH Tree
            //here I can determine thru if statements what generate w/ DGH to run
            //I'm assumming Column to be generalized is 0-3
            dghTrees.get(colToBeGeneralized).setDGHNodeLevels(dghTrees.get(colToBeGeneralized).root
                    , dghTrees.get(colToBeGeneralized).getHeight());
            myPrivateTable = generateTableWithDGHTable(myPrivateTable, dghTrees.get(colToBeGeneralized),
                    colToBeGeneralized);
            freqSet = getFreqSet(myPrivateTable);
        }
        myPrivateTable = suppress(myPrivateTable, kAnon);
        return myPrivateTable;
    }

    /**
     * Attribute values combination and the number of times they occur
     * @param table
     * @return row of quasi identifiers and the number of times they occur
     * All rows are stored in a hashmap
     */
    public HashMap<ArrayList, Integer> getFreqSet(PrivateTable table){
        ArrayList<Integer> quasiColNum = getQuasiColNum(table);
        System.out.println("QuasiColNum: " + quasiColNum);
        //System.out.println("here");
        //check quasi
        int i = 0;
        HashMap<ArrayList, Integer> freqSet = new HashMap<>();
        System.out.println("no of rows in table: " + table.tableRows.size());
        while(i < table.tableRows.size()){
            //get quasiIden for each row
            ArrayList<String> quasiIden = new ArrayList<>();
            //System.out.println("I: " + i);
            for(int x = 0; x < quasiColNum.size(); x++){
                //System.out.println("X: " + x);
                quasiIden.add(table.tableRows.get(i).data.get(x));
                System.out.println("quasiIden " + quasiIden);
            }
            if(freqSet.containsKey(quasiIden)){
                freqSet.replace(quasiIden, freqSet.get(quasiIden),freqSet.get(quasiIden)+ 1);
            }
            else{
                freqSet.put(quasiIden, 1);
            }
            i++;
        }
        return freqSet;
    }

    /**
     * Get the column number of the quasi identifiers
     * @param table
     * @return
     */
    public ArrayList<Integer> getQuasiColNum(PrivateTable table) {
        //I have to get column number to get where the quasi identifiers exist
        //on the table //compare quasi iden to top row header
        ArrayList<Integer> quasiColNum = new ArrayList<>();
        for(int i = 0; i < table.quasiIden.data.size(); i++){
            for(int j = 0; j < table.topRow.getData().size(); j++){
                if((table.quasiIden.data.get(i).compareTo(table.topRow.data.get(j))) == 0){
                    quasiColNum.add(j);
                }
            }
        }
        return quasiColNum;
    }


    /**
     * Old method, assumes values are numeric
     * @param oldTable - modify table with specified generalization
     * @param columnToGeneralize
     * @param generalizationLevel - because I'm assuming numeric data, this determines
     * number
     * @return newTable
     */
    //Domain Hierarchies depend so much on type of data,
    //For now, I will assume quasi-Identifiers are numeric data
    //I may not need DomainGenHier class anymore

    public PrivateTable generateTableWithGen(PrivateTable oldTable, int columnToGeneralize,
                                             int generalizationLevel)
    {
        System.out.println("genLevel " + generalizationLevel);
        PrivateTable newTable = oldTable.copy();
        for(int i = 0; i < oldTable.tableRows.size(); i++){
            //let me store the new value in a String
            String newValue;
            String oldValue = oldTable.tableRows.get(i).data.get(columnToGeneralize);
            if((generalizationLevel > 1))
            {
                String oldValue1 = oldValue.substring(0, oldValue.indexOf('*'));
                String oldValue2 = oldValue.substring(oldValue.indexOf('*'));
                if(oldValue1.length()-generalizationLevel > 0){
                    newValue = oldValue1.substring(0, oldValue1.length()-generalizationLevel
                    ) + "*";
                }
                else{
                    newValue = oldValue1.substring(0, oldValue1.length()) + "*";
                }
                newValue = newValue + oldValue2;
            }
            else{
                newValue = oldValue.substring(0, oldValue.length()-generalizationLevel
                ) + "*";
            }
            newTable.tableRows.get(i).data.set(columnToGeneralize,
                    newValue);
        }
        return newTable;
    }

    /**
     * Makes sure values in a specified column of table have same number of
     * characters
     * @param oldTable
     * @param columnToRectify
     * @return
     */
    public PrivateTable rectifyTableColumn(PrivateTable oldTable, int columnToRectify) throws FileNotFoundException{
        PrivateTable newTable = oldTable.copy();
        int max = newTable.tableRows.get(0).data.get(columnToRectify).length();
        String maxId = "";
        for(int i = 1; i < newTable.tableRows.size(); i++){
            if(newTable.tableRows.get(i).data.get(columnToRectify).length() > max){
                max = newTable.tableRows.get(i).data.get(columnToRectify).length();
                maxId = newTable.tableRows.get(i).data.get(columnToRectify);
            }
        }
        System.out.println("MAXIMUM: " + max);
        for(int i = 0; i < newTable.tableRows.size(); i++){
            if(newTable.tableRows.get(i).data.get(columnToRectify).length() < max){
                String attache = "";
                for(int j = 0; j < (max - newTable.tableRows.get(i).data.get(columnToRectify).length()); j++){
                    attache = attache + "0";
                }
                newTable.tableRows.get(i).data.set(columnToRectify, attache + newTable.tableRows.get(i).data.get(columnToRectify));
            }
        }

        String dghFile = "/Users/valentinlanger/IdeaProjects/dataFly/src/datafly/maximum.txt";
        File infile = new File(dghFile);
        PrintWriter writer = new PrintWriter(infile);
        writer.print(max);
        writer.println();
        writer.print(maxId);
        writer.close();
        return newTable;

    }

    /**
     * Use DGH to generalize a table
     * @param oldTable
     * @param dghTreeCustomized
     * @param columnToGeneralize
     * @return
     * @throws FileNotFoundException
     */
    public PrivateTable generateTableWithDGHTable(PrivateTable oldTable, DGHTreeCustomized dghTreeCustomized, int columnToGeneralize) throws FileNotFoundException{
        PrivateTable newTable = oldTable.copy();
        for(int i = 0; i < oldTable.tableRows.size();i++){
            String newElement = dghTreeCustomized.getGeneralization(newTable.tableRows.get(i).data.get(columnToGeneralize));
            newTable.tableRows.get(i).data.set(columnToGeneralize, newElement);
        }
        return newTable;
    }

    /**
     * Create DGH Trees for a table's quasi identifiers
     * This is entirely based on the quasi Identifiers
     * This method is assuming they are in the order Race,DOB , ID,Sex in the
     * returned table from the database so, 0,1,2,3
     * Has to be modified depending on database and attributes..
     * Some elements are universal
     * @param table - need to read actual values of DOB and ID from the table
     * @return
     */
    public ArrayList<DGHTreeCustomized> createDGHTrees(PrivateTable table) throws FileNotFoundException{
        ArrayList<DGHTreeCustomized> dghTrees = new ArrayList<>();
        String header = "/Users/valentinlanger/IdeaProjects/dataFly/src/datafly/";

        //create DGH for DOB
        ArrayList<String> dates = new ArrayList<>();
        for(int i = 0; i < table.tableRows.size(); i++){
            dates.add(table.tableRows.get(i).data.get(0));
        }
        DGHTreeCustomized dghTreeCustomizedDOB = new DGHTreeCustomized();
        dghTreeCustomizedDOB = dghTreeCustomizedDOB.createAgeDGHTrees(dates);
        dghTreeCustomizedDOB.setWeight(1);
        dghTreeCustomizedDOB.setLabel("DOB");
        dghTreeCustomizedDOB.setHeight();

        dghTreeCustomizedDOB.setDGHNodeLevels(dghTreeCustomizedDOB.root, dghTreeCustomizedDOB.getHeight()-1);
        dghTrees.add(dghTreeCustomizedDOB);

        //create DGH for Workclass
        DGHTreeCustomized dghTreeCustomizedRace = new DGHTreeCustomized(header + "dghWorkclass");
        dghTreeCustomizedRace.setWeight(0);
        dghTreeCustomizedRace.setLabel("Workclass");
        dghTreeCustomizedRace.setHeight();
        dghTreeCustomizedRace.setDGHNodeLevels(dghTreeCustomizedRace.root, dghTreeCustomizedRace.getHeight()-1);
        dghTrees.add(dghTreeCustomizedRace);


        //create DGH for ID
        ArrayList<String> ids = new ArrayList<>();
        for(int i = 0; i < table.tableRows.size(); i++){
            ids.add(table.tableRows.get(i).data.get(2));
        }
        DGHTreeCustomized dghTreeCustomizedID = new DGHTreeCustomized();
        dghTreeCustomizedID = dghTreeCustomizedID.createDGHTree(ids);
        dghTreeCustomizedID.setLabel("ID");
        dghTreeCustomizedID.setWeight(0.5);
        dghTreeCustomizedID.setHeight();
        dghTreeCustomizedID.setDGHNodeLevels(dghTreeCustomizedID.root, dghTreeCustomizedID.getHeight()-1);
        dghTrees.add(dghTreeCustomizedID);

        //create DGH for Education
        DGHTreeCustomized dghTreeCustomizedEducation = new DGHTreeCustomized(header + "dghEducation");
        dghTreeCustomizedEducation.setWeight(0);
        dghTreeCustomizedEducation.setLabel("Education");
        dghTreeCustomizedEducation.setHeight();
        dghTreeCustomizedEducation.setDGHNodeLevels(dghTreeCustomizedEducation.root, dghTreeCustomizedEducation.getHeight()-1);
        dghTrees.add(dghTreeCustomizedEducation);
        /*
        //create DGH for Years of Education
        ArrayList<String> education_years = new ArrayList<>();
        for(int i = 0; i < table.tableRows.size(); i++){
            education_years.add(table.tableRows.get(i).data.get(4));
        }
        DGHTreeCustomized dghTreeEduYears = new DGHTreeCustomized();
        dghTreeEduYears = dghTreeEduYears.createAgeDGHTrees(education_years);
        dghTreeEduYears.setWeight(1);
        dghTreeEduYears.setLabel("Years of Education");
        dghTreeEduYears.setHeight();
        dghTreeEduYears.setDGHNodeLevels(dghTreeEduYears.root, dghTreeEduYears.getHeight()-1);
        dghTrees.add(dghTreeEduYears);

        //create DGH for Sex
        DGHTreeCustomized dghTreeSex = new DGHTreeCustomized(header + "dghSex");
        dghTreeSex.setWeight(0);
        dghTreeSex.setLabel("Sex");
        dghTreeSex.setHeight();
        dghTreeSex.setDGHNodeLevels(dghTreeSex.root, dghTreeSex.getHeight()-1);
        dghTrees.add(dghTreeSex);*/
        return dghTrees;
    }

    /**
     * Find Attribute with most distinct values
     * @param table
     * @param freqList
     * @return
     */
    public int getAttributeWithMostDistinctValues(PrivateTable table, HashMap<ArrayList, Integer> freqList){
        String attribute /*attribute2*/ ;
        int attributeColumn = 0;
        //int attributeColumn2 = 0;
        TableRow quasiId = table.quasiIden;

        //I will make a quasi identifier list of Lists (of all the unique values)
        ArrayList<ArrayList> quasiIden = new ArrayList<>();
        for (int i = 0; i < quasiId.data.size(); i++){
            //it has the list of all values for every quasi identifier column
            quasiIden.add(new ArrayList<>());//wtf does this do again
        }
        ArrayList[] setOfKeys = new ArrayList[freqList.size()];//freqList has distinct keys but it's in row form
        setOfKeys = freqList.keySet().toArray(setOfKeys);
        for(int i = 0; i < setOfKeys.length; i++){
            for(int j = 0; j < setOfKeys[i].size(); j++){
                if(quasiIden.get(j).contains(setOfKeys[i].get(j)) == false)
                    quasiIden.get(j).add(setOfKeys[i].get(j));
            }
        }
        int max = 0;
        //int secondMax = 0;
        for(int i = 0; i < quasiIden.size(); i++){
            if(quasiIden.get(i).size() > max ){
                max = quasiIden.get(i ).size();
                attributeColumn = i;
            }
        }
        System.out.println("attributeColumn - " + attributeColumn);
        attribute = quasiId.data.get(attributeColumn);
        System.out.println(attribute);
        return table.topRow.data.indexOf(attribute);
    }

    /**
     * Checks if sequence of quasi Identifiers in a freqSet >= kAnon
     * @param freqSet
     * @param kAnon
     * @return
     */
    public boolean seqOccursLessThanKTimes(HashMap<ArrayList, Integer> freqSet, int kAnon){
        Integer[] freqValues = new Integer[freqSet.size()];
        freqValues = freqSet.values().toArray(freqValues);
        int noOfTuplesWithDistinctSequences = 0;
        for (int i = 0; i < freqValues.length; i++){
           /*if(freqValues[i] < kAnon)
                return true;*/
            if(freqValues[i] == 1)
            {
                noOfTuplesWithDistinctSequences++;
                //System.out.println("noOfTuplesWithDistinctSequences: " + noOfTuplesWithDistinctSequences);
            }
            if(noOfTuplesWithDistinctSequences >= kAnon)
                return true;
        }
        return false;
    }

    /**
     * Suppresses outliers
     * @param table
     * @param kAnon
     * @return
     */
    public PrivateTable suppress(PrivateTable table, int kAnon){
        /* if max level of generalization is reached, then you suppress ?
        Why do this when I have a while that doesn't let up until generalization is reached, how
        do I combine them
        */
        ArrayList<ArrayList> sequencesToSuppress = new ArrayList<>();
        ArrayList <Integer> quasiIdenCol = getQuasiColNum(table);
        PrivateTable newTable = table.copy();
        HashMap<ArrayList, Integer> freqSet = getFreqSet(newTable);
        ArrayList[] setOfKeys = new ArrayList[freqSet.size()];
        setOfKeys = freqSet.keySet().toArray(setOfKeys);
        for(int i = 0; i < setOfKeys.length; i++){
            if(freqSet.get(setOfKeys[i]) < kAnon){
                sequencesToSuppress.add(setOfKeys[i]);
            }
        }
        //assuming the number of rows to be suppressed be less than kAnon
        for(int j = 0; j < sequencesToSuppress.size(); j++){

            //System.out.println("sequencesToSuppress" + sequencesToSuppress);
            int k = 0;

            while(k < newTable.tableRows.size()){
                int oldSize = newTable.tableRows.size();
                ArrayList<String> quasiIdenVal = new ArrayList<>();
                //= newTable.tableRows.get(k).data;
                for(int m = 0; m < quasiIdenCol.size(); m++){
                    quasiIdenVal.add(newTable.tableRows.get(k).data.get(quasiIdenCol.get(m)));
                }
                if(sequencesToSuppress.get(j).equals(quasiIdenVal)){
                    //System.out.println("k - " + k);
                    newTable.tableRows.remove(k);
                }
                if(oldSize > newTable.tableRows.size())
                {
                    k = 0;
                }else{
                    k++;
                }
                //k++;
            }
        }

        return newTable;
    }

}
