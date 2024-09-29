
package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class CloudSimExampleChatGPT {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;

    public static void main(String[] args) {

        Log.println("Starting Simple Task Scheduler...");
//        Log.println("hostList.add " + new RamProvisionerSimple(ram));
//        assertNotNull(new RamProvisionerSimple(ram));
        try {
            int num_user = 1;  // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);

            // Create Datacenter
            Datacenter datacenter = createDatacenter("Datacenter_0");

            // Create Broker
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            // Create Virtual Machines
            vmList = createVM(brokerId, 3, 1);  // Creating 3 VMs
            broker.submitGuestList(vmList);

            // Create Cloudlets (tasks)
            cloudletList = createCloudlet(brokerId, 6);  // Creating 6 Cloudlets (tasks)
            broker.submitCloudletList(cloudletList);

            // Simple Round-Robin Scheduler to allocate tasks to VMs
            scheduleTasksToVMs(broker, vmList, cloudletList);

            // Start simulation
            CloudSim.startSimulation();

            CloudSim.stopSimulation();

            // Retrieve results
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            printCloudletList(newList);

            Log.println("Simple Task Scheduler finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.println("Unwanted errors occurred");
        }
    }

    // Creates a simple datacenter with one host and multiple VMs
    private static Datacenter createDatacenter(String name) {

        List<Host> hostList = new ArrayList<>();

        // Create 1 host with 2 CPU cores (PEs)
        List<Pe> peList = new ArrayList<>();
        int mips = 1000;
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));
        peList.add(new Pe(1, new PeProvisionerSimple(mips)));

        int hostId = 0;
        int ram = 4096;  // host memory (MB)
        long storage = 1000000;  // host storage
        int bw = 10000;  // bandwidth

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList,
                        new VmSchedulerTimeShared(peList)));


        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;

        LinkedList<Storage> storageList = new LinkedList<>();


        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    // Create VMs for the broker

    /**
     * The vmlist.
     */
    private static List<Vm> createVM(int userId, int vms, int idShift) {
        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<Vm> list = new LinkedList<>();

        //VM Parameters
        long size = 10000; //image size (MB)
        int ram = 512; //vm memory (MB)
        int mips = 250;
        long bw = 1000;
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name

        //create VMs
        Vm[] vm = new Vm[vms];

        for (int i = 0; i < vms; i++) {
            vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            list.add(vm[i]);
        }

        return list;
    }


    // Create Cloudlets (tasks) for the broker
    private static List<Cloudlet> createCloudlet(int brokerId, int numCloudlets) {
        List<Cloudlet> cloudlets = new ArrayList<>();

        // Cloudlet properties
        long length = 400000;  // Task length in million instructions
        long fileSize = 300;  // File size (MB)
        long outputSize = 300;  // Output size (MB)
        int pesNumber = 1;  // Number of CPU cores required
        UtilizationModel utilizationModel = new UtilizationModelFull();

        // Create multiple cloudlets
        for (int i = 0; i < numCloudlets; i++) {
            Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize,
                    utilizationModel, utilizationModel, utilizationModel);
            cloudlet.setUserId(brokerId);
            cloudlets.add(cloudlet);
        }

        return cloudlets;
    }

    // Simple Round-Robin Task Scheduler
    private static void scheduleTasksToVMs(DatacenterBroker broker, List<Vm> vmList, List<Cloudlet> cloudletList) {
        int vmIndex = 0;

        // Assign each cloudlet to a VM in round-robin fashion
        for (Cloudlet cloudlet : cloudletList) {
            Vm vm = vmList.get(vmIndex);  // Select VM in round-robin order
            cloudlet.setVmId(vm.getId());  // Assign the cloudlet to the selected VM
            vmIndex = (vmIndex + 1) % vmList.size();  // Wrap around VM index if it exceeds the number of VMs
        }
    }

    // Print Cloudlet execution results
    private static void printCloudletList(List<Cloudlet> list) {
        String indent = "    ";
        Log.println();
        Log.println("========== OUTPUT ==========");
        Log.println("Cloudlet ID" + indent + "STATUS" + indent +
                "Data center ID" + indent + "VM ID" + indent + "Time" + indent +
                "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");

        for (Cloudlet cloudlet : list) {
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                Log.println("SUCCESS" + indent + indent + cloudlet.getResourceId()
                        + indent + indent + cloudlet.getVmId()
                        + indent + indent + dft.format(cloudlet.getActualCPUTime())
                        + indent + indent + dft.format(cloudlet.getExecStartTime())
                        + indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }
    }

    // Create Broker
    private static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }
}
