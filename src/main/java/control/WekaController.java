package control;

import model.ClassifierInfo;
import model.FileJava;
import model.Issue;
import model.Release;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.SMOTE;

import java.util.ArrayList;
import java.util.List;

public class WekaController {

    private static final String RANDOM_FOREST = "Random Forest";
    private static final String NAIVE_BAYES = "Naive Bayes";
    private static final String IBK = "IBk";

    private final String projName;
    public WekaController(String projName) {

        this.projName = projName;
    }

    public void doWalkForward() throws Exception {

        JiraController jiraControl = new JiraController();

        List<Release> releaseList = jiraControl.getReleases(projName);
        List<Issue> bugsList = jiraControl.getIssues(projName);
        List<Release> halfReleaseList = jiraControl.halfReleases(releaseList);

        //qui applico walk forward richiamando

        for(int countRelease = 1; countRelease < halfReleaseList.size(); countRelease++){

            ProportionController proportionControl = new ProportionController();
            ProportionController.computeProportion(releaseList.subList(0, countRelease), bugsList);
            List<Issue> bugsListFinal = JiraController.cleanOvFv(bugsList);
            proportionControl.calculatorAvAfterProportion(bugsListFinal, releaseList.subList(0, countRelease));
            GitController gitControl = new GitController(projName);
            List<List<FileJava>> fileJavaList = gitControl.loadGitInfo(projName, halfReleaseList.subList(0,countRelease));
            MetricsController metricsControl = new MetricsController(projName);
            List<FileJava> tempListFile = metricsControl.computeBuggynessProva(fileJavaList, bugsListFinal);

            for(List<FileJava> fileJavaL : fileJavaList){
                for(FileJava file : fileJavaL){
                    file.setBuggy("No");
                    if(tempListFile.contains(file)){
                        file.setBuggy("Yes");
                    }
                }
            }

            CsvController csvControl = new CsvController(projName);
            //genero il file csv
            csvControl.makeCsv(fileJavaList, projName, countRelease);
            //converto il csv in arff e genero il file arff
            csvControl.generateArff(projName, countRelease, "training");

        }
    }


    public void computeClassifier(int numberIterator, String projName) throws Exception {

        //liste di classificatori semplici, senza ulteriori parametri
        List<ClassifierInfo> randomForestList = new ArrayList<>();
        List<ClassifierInfo> naiveBayesList = new ArrayList<>();
        List<ClassifierInfo> ibkList = new ArrayList<>();

        //liste con feature selection
        List<ClassifierInfo> featureRandomForestList = new ArrayList<>();
        List<ClassifierInfo> featureNaiveBayesList = new ArrayList<>();
        List<ClassifierInfo> featureIbkList = new ArrayList<>();

        //list con sampling (bilanciamento)
        List<ClassifierInfo> samplingRandomForestList = new ArrayList<>();
        List<ClassifierInfo> samplingNaiveBayesList = new ArrayList<>();
        List<ClassifierInfo> samplingIbkList = new ArrayList<>();

        List<ClassifierInfo> samplingRandomForestListSel = new ArrayList<>();
        List<ClassifierInfo> samplingNaiveBayesListSel = new ArrayList<>();
        List<ClassifierInfo> samplingIbkListSel = new ArrayList<>();

        //list con cost sensitive
        List<ClassifierInfo> costSensRandomForestList = new ArrayList<>();
        List<ClassifierInfo> costSensNaiveBayesList = new ArrayList<>();
        List<ClassifierInfo> costSensIbkList = new ArrayList<>();

        //list con feature sel and cost sensitive
        List<ClassifierInfo> featureCostSensRandomForestList = new ArrayList<>();
        List<ClassifierInfo> featureCostSensNaiveBayesList = new ArrayList<>();
        List<ClassifierInfo> featureCostSensIbkList = new ArrayList<>();

        //list con oversampling and cost sensitive
        List<ClassifierInfo> samplingCostSensRandomForestList = new ArrayList<>();
        List<ClassifierInfo> samplingCostSensNaiveBayesList = new ArrayList<>();
        List<ClassifierInfo> samplingCostSensIbkList = new ArrayList<>();


        int countTest = 3;
        for( int count = 2; count <= numberIterator; count++){

            /*
                in questo ciclo for devo iterare tante volte quante sono le release(metà) per il walk forward
                e mettere i risultati ogni volta nella lista appropriata a quel classificatore
             */

            DataSource source1 = new DataSource(projName + "_training_" + count + ".arff");
            DataSource source2 = new DataSource(projName + "_testing_" + countTest + ".arff");

            countTest++;

            //creo gli oggetti istanze per lavorare su training e testing separatamente
            Instances training = source1.getDataSet();
            Instances testing = source2.getDataSet();

            RandomForest randomForestClassifier = new RandomForest();
            NaiveBayes naiveBayesClassifier = new NaiveBayes();
            IBk ibkClassifier = new IBk();

            int numAttr = training.numAttributes();
            training.setClassIndex(numAttr - 1);
            testing.setClassIndex(numAttr - 1);

            Evaluation eval = new Evaluation(testing);

            //blocco try-catch per problema quando tutti i buggy sono NO nell'ultimo testing
            try {
                randomForestClassifier.buildClassifier(training);
                eval.evaluateModel(randomForestClassifier, testing);
                ClassifierInfo simpleRandomForest = new ClassifierInfo(this.projName, count, RANDOM_FOREST, false, false, false);
                setSimpleClassifier(simpleRandomForest, eval, training, testing, randomForestList);

                eval = new Evaluation(testing);
                naiveBayesClassifier.buildClassifier(training);
                eval.evaluateModel(naiveBayesClassifier, testing);
                ClassifierInfo simpleNaiveBayes = new ClassifierInfo(this.projName, count, NAIVE_BAYES, false, false, false);
                setSimpleClassifier(simpleNaiveBayes, eval, training, testing, naiveBayesList);

                eval = new Evaluation(testing);
                ibkClassifier.buildClassifier(training);
                eval.evaluateModel(ibkClassifier, testing);
                ClassifierInfo simpleIBk = new ClassifierInfo(this.projName, count, IBK, false, false, false);
                setSimpleClassifier(simpleIBk, eval, training, testing, ibkList);

            //////--------inizio feature selection con BEST FIRST
                BestFirst search = new BestFirst();
                CfsSubsetEval evalSub = new CfsSubsetEval();

                AttributeSelection filter = new AttributeSelection();
                filter.setEvaluator(evalSub);
                filter.setSearch(search);
                filter.setInputFormat(training);

                Instances filteredTraining = Filter.useFilter(training, filter);
                Instances filteredTesting = Filter.useFilter(testing, filter);

                int numAttrFiltered = filteredTraining.numAttributes();
                filteredTraining.setClassIndex(numAttrFiltered - 1);

                eval = new Evaluation(testing);
                randomForestClassifier.buildClassifier(filteredTraining);
                eval.evaluateModel(randomForestClassifier, filteredTesting);
                ClassifierInfo featureRandomForest = new ClassifierInfo(this.projName, count, RANDOM_FOREST, true, false, false);
                setSimpleClassifier(featureRandomForest, eval, filteredTraining, filteredTesting, featureRandomForestList);

                eval = new Evaluation(testing);
                naiveBayesClassifier.buildClassifier(filteredTraining);
                eval.evaluateModel(naiveBayesClassifier, filteredTesting);
                ClassifierInfo featureNaiveBayes = new ClassifierInfo(this.projName, count, NAIVE_BAYES, true, false, false);
                setSimpleClassifier(featureNaiveBayes, eval, filteredTraining, filteredTesting, featureNaiveBayesList);

                eval = new Evaluation(testing);
                ibkClassifier.buildClassifier(filteredTraining);
                eval.evaluateModel(naiveBayesClassifier, filteredTesting);
                ClassifierInfo featureIbk = new ClassifierInfo(this.projName, count, IBK, true, false, false);
                setSimpleClassifier(featureIbk, eval, filteredTraining, filteredTesting, featureIbkList);

                ////--- feature selection con cost sensitive

                CostMatrix costMatrix2 = new CostMatrix(2); //matrice 2x2 con CFN = 10*CFP, ovvero classificare erroneamente
                //un'istanza negativa come positiva ci costa 10 rispetto al contrario che costa 1
                costMatrix2.setCell(0,0,0.0);
                costMatrix2.setCell(1,0, 10.0);
                costMatrix2.setCell(0,1,1.0);
                costMatrix2.setCell(1,1,0.0);

                eval = new Evaluation(filteredTesting);

                CostSensitiveClassifier featureCostSensitiveClassifier = new CostSensitiveClassifier();
                //random forest cost sens and feature sel
                featureCostSensitiveClassifier.setClassifier(randomForestClassifier);
                featureCostSensitiveClassifier.setCostMatrix(costMatrix2);
                featureCostSensitiveClassifier.buildClassifier(filteredTraining);
                eval.evaluateModel(featureCostSensitiveClassifier, filteredTesting);
                ClassifierInfo featureCostSensRandomForest = new ClassifierInfo(this.projName, count, RANDOM_FOREST, true, false, true);
                setSimpleClassifier(featureCostSensRandomForest, eval, filteredTraining, filteredTesting, featureCostSensRandomForestList);

                //naive Bayes cost sens and feature sel
                eval = new Evaluation(filteredTesting);
                featureCostSensitiveClassifier.setClassifier(naiveBayesClassifier);
                featureCostSensitiveClassifier.setCostMatrix(costMatrix2);
                featureCostSensitiveClassifier.buildClassifier(filteredTraining);
                eval.evaluateModel(featureCostSensitiveClassifier, filteredTesting);
                ClassifierInfo featureCostSensNaiveBayes = new ClassifierInfo(this.projName, count, NAIVE_BAYES, true, false, true);
                setSimpleClassifier(featureCostSensNaiveBayes, eval, filteredTraining, filteredTesting, featureCostSensNaiveBayesList);

                //ibk cost sens and feature sel
                eval = new Evaluation(filteredTesting);
                featureCostSensitiveClassifier.setClassifier(ibkClassifier);
                featureCostSensitiveClassifier.setCostMatrix(costMatrix2);
                featureCostSensitiveClassifier.buildClassifier(filteredTraining);
                eval.evaluateModel(featureCostSensitiveClassifier, filteredTesting);
                ClassifierInfo featureCostSensIbk = new ClassifierInfo(this.projName, count, IBK, true, false, true);
                setSimpleClassifier(featureCostSensIbk, eval, filteredTraining, filteredTesting, featureCostSensIbkList);


            ////---- inizio sampling con OVERSAMPLING ma senza feature selection

                SMOTE smoteFilter = new SMOTE();
                smoteFilter.setInputFormat(training);
                Instances oversampledData = Filter.useFilter(training, smoteFilter); //faccio solo il training perché sto facendo OVER

                eval = new Evaluation(testing);
                randomForestClassifier.buildClassifier(oversampledData);
                eval.evaluateModel(randomForestClassifier, testing);
                ClassifierInfo overSamplingRandomForest = new ClassifierInfo(this.projName, count, RANDOM_FOREST, false, true, false);
                setSimpleClassifier(overSamplingRandomForest, eval, oversampledData, testing, samplingRandomForestList);

                eval = new Evaluation(testing);
                naiveBayesClassifier.buildClassifier(oversampledData);
                eval.evaluateModel(naiveBayesClassifier, testing);
                ClassifierInfo overSamplingNaiveBayes = new ClassifierInfo(this.projName, count, NAIVE_BAYES, false, true, false);
                setSimpleClassifier(overSamplingNaiveBayes, eval, oversampledData, testing, samplingNaiveBayesList);

                eval = new Evaluation(testing);
                ibkClassifier.buildClassifier(oversampledData);
                eval.evaluateModel(ibkClassifier, testing);
                ClassifierInfo overSamplingIbk = new ClassifierInfo(this.projName, count, IBK, false, true, false);
                setSimpleClassifier(overSamplingIbk, eval, oversampledData, testing, samplingIbkList);

                //---- inizio feature selection and OVERSAMPLING

                if (filteredTraining.numInstances() > 0 && filteredTesting.numInstances() > 0) {

                    SMOTE smoteFilterSel = new SMOTE();
                    smoteFilterSel.setOptions(new String[]{"-M", "1.0"});
                    smoteFilterSel.setInputFormat(filteredTraining);
                    Instances oversampledDataSel = Filter.useFilter(filteredTraining, smoteFilterSel);


                    //random forest
                    eval = new Evaluation(testing);
                    randomForestClassifier.buildClassifier(oversampledDataSel);
                    eval.evaluateModel(randomForestClassifier, filteredTesting);
                    ClassifierInfo overSamplingRandomForestSel = new ClassifierInfo(this.projName, count, RANDOM_FOREST, true, true, false);
                    setSimpleClassifier(overSamplingRandomForestSel, eval, oversampledDataSel, filteredTesting, samplingRandomForestListSel);

                    //naive bayes
                    eval = new Evaluation(testing);
                    naiveBayesClassifier.buildClassifier(oversampledDataSel);
                    eval.evaluateModel(naiveBayesClassifier, filteredTesting);
                    ClassifierInfo overSamplingNaiveBayesSel = new ClassifierInfo(this.projName, count, NAIVE_BAYES, true, true, false);
                    setSimpleClassifier(overSamplingNaiveBayesSel, eval, oversampledDataSel, filteredTesting, samplingNaiveBayesListSel);
                    //ibk
                    eval = new Evaluation(testing);
                    ibkClassifier.buildClassifier(oversampledDataSel);
                    eval.evaluateModel(ibkClassifier, filteredTesting);
                    ClassifierInfo overSamplingIbkSel = new ClassifierInfo(this.projName, count, IBK, true, true, false);
                    setSimpleClassifier(overSamplingIbkSel, eval, oversampledDataSel, filteredTesting, samplingIbkListSel);

                }

                /* Inizio cost sensitive
                La cella (0,0) rappresenta il costo di classificare correttamente un'istanza negativa,
                    che in questo caso è impostato a 0.
                La cella (1,0) rappresenta il costo di classificare erroneamente un'istanza negativa come positiva,
                    che in questo caso è impostato a 10.
                La cella (0,1) rappresenta il costo di classificare erroneamente un'istanza positiva come negativa,
                    che in questo caso è impostato a 1.
                Infine, la cella (1,1) rappresenta il costo di classificare correttamente un'istanza positiva,
                    che in questo caso è impostato a 0.
                 */
                CostMatrix costMatrix = new CostMatrix(2); //matrice 2x2 con CFN = 10*CFP, ovvero classificare erroneamente
                                                                        //un'istanza negativa come positiva ci costa 10 rispetto al contrario che costa 1
                costMatrix.setCell(0,0,0.0);
                costMatrix.setCell(1,0, 10.0);
                costMatrix.setCell(0,1,1.0);
                costMatrix.setCell(1,1,0.0);

                eval = new Evaluation(testing);

                CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
                //random forest cost sens
                costSensitiveClassifier.setClassifier(randomForestClassifier);
                costSensitiveClassifier.setCostMatrix(costMatrix);
                costSensitiveClassifier.buildClassifier(training);
                eval.evaluateModel(costSensitiveClassifier, testing);
                ClassifierInfo costSensRandomForest = new ClassifierInfo(this.projName, count, RANDOM_FOREST, false, false, true);
                setSimpleClassifier(costSensRandomForest, eval, training, testing, costSensRandomForestList);

                //naive Bayes cost sens
                eval = new Evaluation(testing);
                costSensitiveClassifier.setClassifier(naiveBayesClassifier);
                costSensitiveClassifier.setCostMatrix(costMatrix);
                costSensitiveClassifier.buildClassifier(training);
                eval.evaluateModel(costSensitiveClassifier, testing);
                ClassifierInfo costSensNaiveBayes = new ClassifierInfo(this.projName, count, NAIVE_BAYES, false, false, true);
                setSimpleClassifier(costSensNaiveBayes, eval, training, testing, costSensNaiveBayesList);

                //ibk cost sens
                eval = new Evaluation(testing);
                costSensitiveClassifier.setClassifier(ibkClassifier);
                costSensitiveClassifier.setCostMatrix(costMatrix);
                costSensitiveClassifier.buildClassifier(training);
                eval.evaluateModel(costSensitiveClassifier, testing);
                ClassifierInfo costSensIbk = new ClassifierInfo(this.projName, count, IBK, false, false, true);
                setSimpleClassifier(costSensIbk, eval, training, testing, costSensIbkList);

                ///--- inizio oversampling and cost sensitive

                eval = new Evaluation(testing);

                CostSensitiveClassifier samplingCostSensitiveClassifier = new CostSensitiveClassifier();
                //random forest oversampling and cost sens
                samplingCostSensitiveClassifier.setClassifier(randomForestClassifier);
                samplingCostSensitiveClassifier.setCostMatrix(costMatrix);
                samplingCostSensitiveClassifier.buildClassifier(oversampledData);
                eval.evaluateModel(samplingCostSensitiveClassifier, testing);
                ClassifierInfo samplingCostSensRandomForest = new ClassifierInfo(this.projName, count, RANDOM_FOREST, false, true, true);
                setSimpleClassifier(samplingCostSensRandomForest, eval, oversampledData, testing, samplingCostSensRandomForestList);

                //naive Bayes oversampling and cost sens
                eval = new Evaluation(testing);
                samplingCostSensitiveClassifier.setClassifier(naiveBayesClassifier);
                samplingCostSensitiveClassifier.setCostMatrix(costMatrix);
                samplingCostSensitiveClassifier.buildClassifier(oversampledData);
                eval.evaluateModel(samplingCostSensitiveClassifier, testing);
                ClassifierInfo samplingCostSensNaiveBayes = new ClassifierInfo(this.projName, count, NAIVE_BAYES, false, true, true);
                setSimpleClassifier(samplingCostSensNaiveBayes, eval, oversampledData, testing, samplingCostSensNaiveBayesList);

                //ibk oversampling and cost sens
                eval = new Evaluation(testing);
                samplingCostSensitiveClassifier.setClassifier(ibkClassifier);
                samplingCostSensitiveClassifier.setCostMatrix(costMatrix);
                samplingCostSensitiveClassifier.buildClassifier(oversampledData);
                eval.evaluateModel(samplingCostSensitiveClassifier, testing);
                ClassifierInfo samplingCostSensIbk = new ClassifierInfo(this.projName, count, IBK, false, true, true);
                setSimpleClassifier(samplingCostSensIbk, eval, oversampledData, testing, samplingCostSensIbkList);

            } catch (Exception e){
                //eccezione non gestita
            }

        } //fine for

        List<List<ClassifierInfo>> allRandomForest = new ArrayList<>();
        allRandomForest.add(randomForestList);
        allRandomForest.add(featureRandomForestList);
        allRandomForest.add(samplingRandomForestList);
        allRandomForest.add(samplingRandomForestListSel);
        allRandomForest.add(costSensRandomForestList);
        allRandomForest.add(featureCostSensRandomForestList);
        allRandomForest.add(samplingCostSensRandomForestList);


        List<List<ClassifierInfo>> allNaiveBayes = new ArrayList<>();
        allNaiveBayes.add(naiveBayesList);
        allNaiveBayes.add(featureNaiveBayesList);
        allNaiveBayes.add(samplingNaiveBayesList);
        allNaiveBayes.add(samplingNaiveBayesListSel);
        allNaiveBayes.add(costSensNaiveBayesList);
        allNaiveBayes.add(featureCostSensNaiveBayesList);
        allNaiveBayes.add(samplingCostSensNaiveBayesList);


        List<List<ClassifierInfo>> allIbk = new ArrayList<>();
        allIbk.add(ibkList);
        allIbk.add(featureIbkList);
        allIbk.add(samplingIbkList);
        allIbk.add(samplingIbkListSel);
        allIbk.add(costSensIbkList);
        allIbk.add(featureCostSensIbkList);
        allIbk.add(samplingCostSensIbkList);



        CsvController csvControl = new CsvController(projName);
        csvControl.makeCsvForReport(allRandomForest);
        csvControl.makeCsvForReport(allNaiveBayes);
        csvControl.makeCsvForReport(allIbk);

    }

    public void setSimpleClassifier(ClassifierInfo classifier, Evaluation eval, Instances training, Instances testing, List<ClassifierInfo> listClassifier ){

        classifier.setTrainingPercent(100.0 * training.numInstances() / (training.numInstances() + testing.numInstances()));
        classifier.setPrecision(eval.precision(0));
        classifier.setRecall(eval.recall(0));
        classifier.setAuc(eval.areaUnderROC(0));
        classifier.setKappa(eval.kappa());
        classifier.setTp(eval.numTruePositives(0));
        classifier.setFp(eval.numFalsePositives(0));
        classifier.setTn(eval.numTrueNegatives(0));
        classifier.setFn(eval.numFalseNegatives(0));
        listClassifier.add(classifier);
    }


}

