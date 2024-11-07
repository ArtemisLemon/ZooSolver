package mde;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.m2m.atl.core.emf.EMFModel;
import org.eclipse.m2m.atl.core.emf.EMFModelFactory;
// import org.eclipse.m2m.atl.emftvm.Model;
import org.eclipse.m2m.atl.emftvm.compiler.AtlResourceFactoryImpl;
import org.eclipse.m2m.atl.engine.parser.AtlParser;
import org.eclipse.xtext.xbase.lib.Exceptions;

import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.stream.IntStream;
import java.nio.file.Path;

import org.oclinchoco.ReferenceTable;
import org.chocosolver.solver.*;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import zoo.*; //This is the model we're trying to solve the problems for, it's designed in xcore
import mde.MVIPropagator;

public class App {
    static int magic = 100;
    static List<IntVar[]> cage2animal_LinkVars; 
    static java.util.Hashtable<EReference,IntVar[]> eRef2LinkVar = new Hashtable<>();
    static java.util.Hashtable<Cage,IntVar[]> cage2animal = new Hashtable<>();
    static java.util.Hashtable<Animal,IntVar[]> animal2cage = new Hashtable<>();
    static java.util.Hashtable<Animal,IntVar[]> animal2species = new Hashtable<>(); //all consts
    static IntVar[][] animals2species_table;
    static Model m = new Model();
    static int cA,cC,cS; //count of Animals, Cages, Species

    static List<IntVar[]> problemVars;

    //XMI Loader
    static EObject loadInstance(String path){
        ResourceSetImpl rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        rs.getPackageRegistry().put(ZooPackage.eNS_URI,ZooPackage.eINSTANCE);
        Resource res = rs.getResource(URI.createFileURI(path), true);
        return res.getContents().get(0);
    }


    // // //ATL Loader
    // static Module loadModule(String path) {
    //     var rs = new ResourceSetImpl();
    //     rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("atl", new AtlResourceFactoryImpl());

    //     var parser = AtlParser.getDefault();
    //     var modelFactory = new EMFModelFactory();
    //     EMFModel problems = (EMFModel)modelFactory.newModel(parser.getProblemMetamodel());
    //     rs.getLoadOptions().put("problems", problems);

    //     try {

    //         var transfo = rs.getResource(URI.createURI(path), true);
    //         var fileName = Path.of(path).getFileName();

    //         for (var e : transfo.getErrors()) {
    //             System.err.println("error in " + fileName + ": " + e.getLine() + "-" + e.getColumn() + " - " + e.getMessage());
    //         }

    //         for (var e : transfo.getWarnings()) {
    //             System.err.println("warning in " + fileName + ": " + e.getLine() + "-" + e.getColumn() + " - " + e.getMessage());
    //         }

    //         return (Module)transfo.getContents().get(0);

    //     } catch (Exception e) {
    //         System.err.println("Error reading input file '" + path + "' : " + e.getMessage());
    //         for (var p : problems.getResource().getContents()) {
    //             if (p instanceof Problem pb) {
    //                 System.out.println(pb.getSeverity() + ": " + pb.getLocation() + " - " + pb.getDescription());
    //             }
    //         }
    //     }
        
    //     return null;
    // }



    //Choco
    static IntVar[] makeLinkVar(Model m, int maxCard, int minCard, int numberOfTargets){ //,int data)
        int lb=0;
        int ub=numberOfTargets;
        if (minCard==maxCard) //get null pointer out of the domain
            ub--; //null pointer is 0 or numberOfTargets, depending on where you start counting
            // lb++; //it's one XOR the other
        return m.intVarArray(maxCard, lb,ub);
    }

    static void initLinkVar(IntVar[] linkvar, int[] data, int n){ //-1 is no data
        initLinkVar(linkvar, data, data, n);
    }

    static void initLinkVar(IntVar[] linkvar, int[] lb, int[] ub, int n){
        for(int i=0;i<n;i++){
            if(lb[i]!=-1 & ub[i]!=-1) try {
                linkvar[i].updateBounds(lb[i], ub[i], null);
            } catch (Exception e){}
        }
    }

    static void oppositeCSP(Model m, IntVar[][] a, IntVar[][] b){
        int al = a.length;
        int bl = b.length;
        IntVar[][] aocc = m.intVarMatrix(al, bl, 0,magic);
        int[] avals = new int[bl];
        for(int i=0;i<bl;i++) avals[i]=(i);
        for(int i=0;i<al;i++){
            m.globalCardinality(a[i],avals,aocc[i],false).post();
        }
        
        IntVar[][] bocc = m.intVarMatrix(bl, al, 0,magic);
        int[] bvals = new int[al];
        for(int i=0;i<al;i++) bvals[i]=(i);
        for(int i=0;i<bl;i++){
            m.globalCardinality(b[i],bvals,bocc[i],false).post();
        }

        for(int i=0;i<al;i++) for(int j=0;j<bl;j++){
            m.ifOnlyIf(m.arithm(aocc[i][j], ">",0), m.arithm(bocc[j][i], ">",0));
        }
    }

    static IntVar[] navCSP(Model m, IntVar[] source, IntVar[][] sources, int s, int ss, int lb, int ub, IntVar dummy){
        int sss = s*ss;
        IntVar[] out = m.intVarArray(sss,lb,ub);
        IntVar[] dummies = new IntVar[ss]; for(int i=0;i<ss;i++) dummies[i] = dummy;//copy dummy ss times
        IntVar[] table = ArrayUtils.concat(ArrayUtils.flatten(sources),dummies); //flatten sources, dummies at the end (ub--)
        // IntVar[] table = ArrayUtils.concat(dummies, ArrayUtils.flatten(sources)); //flatten sources, dummies at the end (lb++)
        
        // for(int i=0;i<sss;i++) m.element(out[i], table, pointer,0);
        int k=0;
        for(int i=0; i<s;i++) for(int j=0;j<ss;j++){
            IntVar pointer = source[i].mul(ss).add(j).intVar(); // = pointer arithm
            m.element(out[k++], table, pointer,0).post();
        }
        return out;
    }

    // static IntVar size(Model m, IntVar[] var, int maxCard, int dummy){
    //     if (var.length == maxCard) return sizeLINK(m, var, maxCard, dummy); //this would work, if the number-of-targets and maxCard can't be equal.
    //     return sizeOCC(m, var, maxCard, dummy);
    // }

    static IntVar sizeOCC(Model m, IntVar[] occVar, int maxCard, int dummy){
        IntVar darc = occVar[dummy];
        return darc.mul(-1).add(maxCard).intVar();
    }

    static IntVar sizeLINK(Model m, IntVar[] linkVar, int maxCard, int dummy){
        IntVar darc = m.count("",dummy, linkVar);
        return darc.mul(-1).add(maxCard).intVar();
    }


    //UML Constraints
    static void uml2CSP(Model m, List<Cage> cages, List<Animal> animals, List<Species> species){
        // java.util.Hashtable<Cage,IntVar[]> cage2animal = new Hashtable<>();
        // java.util.Hashtable<Animal,IntVar[]> animal2cage = new Hashtable<>();
        // java.util.Hashtable<Animal,IntVar[]> animal2species = new Hashtable<>(); //all consts
        // java.util.Hashtable<EReference,IntVar[]> eRef2LinkVar;

        cA=animals.size();
        cC=cages.size();
        cS=species.size();

        // make Vars
        EReference ec0 = cages.get(0).eClass().getEReferences().getFirst();
        // System.out.println(ec0.getLowerBound());
        // System.out.println(ec0.getUpperBound());
        cage2animal_LinkVars = new ArrayList<>();
        for(var c : cages){
            IntVar[] linkVar = makeLinkVar(m, ec0.getUpperBound(), ec0.getLowerBound(), animals.size());
            cage2animal.put(c, linkVar);
            cage2animal_LinkVars.add(linkVar);
            eRef2LinkVar.put(c.eClass().getEReferences().getFirst(),linkVar);
            //init data
            int i=0;
            for(var a : c.getAnimals()){
                m.arithm(linkVar[i++],"=",animals.indexOf(a)).post();
            }
        }
        
        EReference ec = animals.get(0).eClass().getEReferences().getLast();
        // System.out.println(ec.getLowerBound());
        // System.out.println(ec.getUpperBound());
        List<IntVar[]> animal2cage_LinkVars = new ArrayList<>();
        for(var a : animals){
            IntVar[] linkVar = makeLinkVar(m, ec.getUpperBound(), ec.getLowerBound(), cages.size()); 
            animal2cage.put(a,linkVar);
            animal2cage_LinkVars.add(linkVar);
        }
        
        //apply Opposite, you look between all pairs (because only one can be opposite you can eliminate options as you go)
        if(ec0.getEOpposite() == ec) //here True
            oppositeCSP(m,
                cage2animal_LinkVars.toArray(new IntVar[cage2animal_LinkVars.size()][]), 
                animal2cage_LinkVars.toArray(new IntVar[animal2cage_LinkVars.size()][]));


        ec = animals.get(0).eClass().getEReferences().getFirst();
        List<IntVar[]> animal2species_LinkVars = new ArrayList<>();
        // System.out.println(ec.getLowerBound());
        // System.out.println(ec.getUpperBound());
        // System.out.println(species.size());
        for(var a : animals){
            IntVar[] out = makeLinkVar(m, ec.getUpperBound(), ec.getLowerBound(), species.size());
            animal2species_LinkVars.add(out);
            //init data
            m.arithm(out[0],"=",species.indexOf( a.getSpec())).post();
            animal2species.put(a,out); //link to a
        }
        animals2species_table = animal2species_LinkVars.toArray(new IntVar[animal2species_LinkVars.size()][]);
        //return everything nice and sorted
    }

    //OCL Constraint 1: cage.animals.size() =< cage.capacity
    static void ocl_capacity(List<Cage> c){
        for(var cc : c){
            capacity(cage2animal.get(cc), cc.getCapacity());
        }
    }

    //OCL Constraint 1.a: LinkVar.size() =< n
    static void capacity(IntVar[] source, int n){
        //size
        IntVar size = sizeLINK(m, source, source.length, cA);
        //arithm
        m.arithm(size, "<=", n).post();
    }

    // //OCL Constraint 2: cage.animals.species.asSet.size() =< 1
    static void ocl_species(List<Cage> cages){
        IntVar dS = m.intVar(cS);
        for(var c:cages){
            IntVar[] local_animal2species = navCSP(m, cage2animal.get(c), animals2species_table, 10, 1, 0, magic, dS);
            asSetLessThanN(m, local_animal2species, 10, 1, 0, cS, cS);
        }
    }


    // //OCL Constraint 2.a: LinkVar.asSet.size() =< n
    static void asSetLessThanN(Model m, IntVar[] source, int s, int n, int lb, int ub, int dummy){ //the complicated one
        //speciesLINK gcc speciesOCC
        IntVar[] speciesOCC = m.intVarArray("specOCC",cS+1, 0, magic);
        int[] gccIDs = IntStream.range(0, cS+1).toArray();
        m.globalCardinality(source, gccIDs, speciesOCC, true).post();;
        
        //speciesOCC includes asSetOCC
        IntVar[] asSetOCC = m.intVarArray("asSetOCC",cS+1, 0,magic); //domain 0..1 except for asSetOCC[dummy]
        m.sum(asSetOCC,"=",cS).post();
        for(int i=0;i<cS;i++) {
            m.member(asSetOCC[i], 0,1).post();
            // try {
            //     asSetOCC[i].updateBounds(0, 1, null);
            // } catch(Exception e){}
            Constraint l2r = new Constraint("l2r", new MVIPropagator(speciesOCC[i],asSetOCC[i]));
            Constraint r2l = new Constraint("r2l", new MVIPropagator(asSetOCC[i],speciesOCC[i]));
            l2r.post();
            r2l.post();
        }
        Constraint dm = new Constraint("r2l", new MVIPropagator(asSetOCC[cS],speciesOCC[cS]));
        dm.post();

        //asSetOCC size
        IntVar size = sizeOCC(m, asSetOCC, cS, cS);

        //size less than n
        m.arithm(size, "<=", n).post();
        
    }

    static void printCages(Park p){
        System.out.println("Cages #######");
        for (Cage c : p.getCages()){
            System.out.println("##### "+c.getName()+", capacity: "+c.getAnimals().size()+"/"+c.getCapacity());
            for(Animal a : c.getAnimals()){
                System.out.println(a.getName()+" : "+a.getSpec().getName());
            }
            System.out.println("#####################");
        }
    }



    //ECore <-> Choco: OCL Environement
    public static void main(String[] args) {
        //Make a Zoo with ZooBuilder
        ZooBuilder zooBuilder = new ZooBuilder();
        // zooBuilder.makezoofile0(); // 2cages 3 lions 2 gnou
        // zooBuilder.makezoofile1(6); //3cages 3 lions 2 gnou n capybara                               //5->3:39 then n=6 in less time (like 26s)?!?!
        // zooBuilder.makezoofile2(8); //3cages 3 lions 2 gnou n capybara, a lion and a gnou in a cage   //8->1s, 9->54s

        //Load a Zoo
        Park p2 = (Park) loadInstance("myZoo.xmi");
        ResourceSetImpl rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        rs.getPackageRegistry().put(ZooPackage.eNS_URI,ZooPackage.eINSTANCE);
        // Resource res = rs.getResource(URI.createFileURI("myZoo.xmi"), true);
        // Park p2 = (Park) res.getContents().get(0);

        System.out.println("Animals ####");
        for(Animal a : p2.getAnimals()){
            String cagestat=" in a cage";
            if(a.getCage()==null) cagestat="";
            System.out.println(a.getName()+" : "+a.getSpec().getName()+cagestat);
        }
        printCages(p2);

        // Here we make our Orders of Objects, List<> provides indexOf
        List<Animal> csp_animals = p2.getAnimals(); 
        List<Cage> csp_cages = p2.getCages();
        List<Species> csp_species = p2.getSpecs();
        uml2CSP(m, csp_cages, csp_animals, csp_species);
        ocl_capacity(csp_cages);
        ocl_species(csp_cages);


        IntVar[][] problemVars = cage2animal_LinkVars.toArray(new IntVar[cage2animal_LinkVars.size()][]);
        m.getSolver().setSearch(Search.intVarSearch(ArrayUtils.flatten(problemVars)));
        System.out.println("Solving");
        Solution solution = m.getSolver().findSolution();
        if(solution != null){
            System.out.println(solution.toString());
            for(var c:csp_cages){
                int maxCard = c.eClass().getEReferences().getFirst().getUpperBound();
                int[] values = new int[maxCard];
                IntVar[] linkVar = cage2animal.get(c);
                for(int i=0;i<maxCard;i++){
                    values[i] = linkVar[i].getValue();
                    // System.out.println(values[i]);
                    if(values[i]!=cA) zooBuilder.putInCage(csp_animals.get(values[i]), c);
                }
            }
        }
        System.out.println("#######################");
        System.out.println("Zoo Config ############");
        printCages(p2);

        Resource res2 = rs.createResource(URI.createFileURI("myZooConfig.xmi"));
        res2.getContents().add(p2);
        try{
            res2.save(null);
        } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
        }
    }
}