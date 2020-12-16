package btindexmodels.facetedsearchmodels;

import java.util.ArrayList;

public class AvailableFacets {

    public ArrayList<AvailableFacet> incomingConnections;
    public ArrayList<AvailableFacet> outgoingConnections;

    public AvailableFacets() {
        incomingConnections = new ArrayList<>();
        outgoingConnections = new ArrayList<>();
    }
}
