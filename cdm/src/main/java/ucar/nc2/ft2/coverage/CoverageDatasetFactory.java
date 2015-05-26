/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.nc2.ft2.coverage.grid.adapter.DtGridCoverageAdapter;
import ucar.nc2.ft2.coverage.grid.GridCoverageDataset;
import ucar.nc2.ft2.coverage.grid.adapter.GeoGridDataset;
import ucar.nc2.ft2.remote.CdmrFeatureDataset;

import java.io.IOException;

/**
 * factory for CoverageDataset
 *
 * @author caron
 * @since 5/26/2015
 */
public class CoverageDatasetFactory {

  static public GridCoverageDataset openGridCoverage(String endpoint) throws IOException {

    if (endpoint.startsWith(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME)) {

      endpoint = endpoint.substring(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME.length());
      CdmrFeatureDataset reader = new CdmrFeatureDataset(endpoint);
      return reader.openGridCoverage();

    } else if (endpoint.startsWith("http:")) {
      CdmrFeatureDataset reader = new CdmrFeatureDataset(endpoint);
      return reader.openGridCoverage();

    } else if (endpoint.startsWith("file:")) {
      endpoint = endpoint.substring("file:".length());
    }

    GeoGridDataset gds = GeoGridDataset.open(endpoint);
    if (gds.getGrids().size() > 0)
      return new DtGridCoverageAdapter(gds);


    return null;
  }
}
