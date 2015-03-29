/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thredds.server.catalog.FeatureCollection;
import thredds.servlet.ServletUtil;
import thredds.util.TdsPathUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.Set;

/**
 * Open the requested dataset as a FeatureDataset/GridDataset/NetcdfFile.
 * If the request requires an authentication challenge, a challenge will be sent back to the client using
 * the response object, and this method will return null.  (This is the only
 * circumstance in which this method will return null.)
 * The client will then repeat the request.
 */
@Component
public class TdsRequestedDataset {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TdsRequestedDataset.class);

  @Autowired
  private static DatasetManager datasetManager;

  @Autowired
  private static DataRootManager dataRootManager;

  public static FeatureCollection getFeatureCollection(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
    TdsRequestedDataset trd = new TdsRequestedDataset(request);
    if (path != null) trd.path = path;
    return trd.openAsFeatureCollection(request, response);
  }

  public static FeatureDataset getFeatureDataset(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
    TdsRequestedDataset trd = new TdsRequestedDataset(request);
    if (path != null) trd.path = path;
    return trd.openAsFeatureDataset(request, response);
  }

  public static GridDataset getGridDataset(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
    TdsRequestedDataset trd = new TdsRequestedDataset(request);
    if (path != null) trd.path = path;
    return trd.openAsGridDataset(request, response);
  }

  public static NetcdfFile getNetcdfFile(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
    TdsRequestedDataset trd = new TdsRequestedDataset(request);
    if (path != null) trd.path = path;
    return trd.openAsNetcdfFile(request, response);
  }

  public static long getLastModified(String reqPath) {
    File file = getFile(reqPath);
    return (file == null) ? -1 : file.lastModified();
  }

  public static File getFile(String reqPath) {
    String location = dataRootManager.getLocationFromRequestPath(reqPath);
    return (location == null) ? null : new File(location);
  }

  public static boolean resourceControlOk(HttpServletRequest request, HttpServletResponse response, String path) {
    return datasetManager.resourceControlOk(request, response, path);
  }


  ///////////////////////////////////////////////////////////////////////////////////
  private boolean isRemote = false;
  private String path;

  public TdsRequestedDataset(HttpServletRequest request) throws IOException {
    this.path = TdsPathUtils.extractPath(request, null);
    if (this.path == null) {
      this.path = ServletUtil.getParameterIgnoreCase(request, "dataset");
      isRemote = (path != null);
    }
    if (this.path == null) {
      throw new FileNotFoundException("Request does not specify a dataset.");
    }
  }

  public FeatureCollection openAsFeatureCollection(HttpServletRequest request, HttpServletResponse response) throws IOException {
    return datasetManager.getFeatureCollection(request, response, path);
  }

  public FeatureDataset openAsFeatureDataset(HttpServletRequest request, HttpServletResponse response) throws IOException {
    return datasetManager.getFeatureDataset(request, response, path);
  }

  public GridDataset openAsGridDataset(HttpServletRequest request, HttpServletResponse response) throws IOException {
    return isRemote ? ucar.nc2.dt.grid.GridDataset.open(path) : datasetManager.openGridDataset(request, response, path);
  }

  public NetcdfFile openAsNetcdfFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
    return isRemote ? NetcdfDataset.openDataset(path) : datasetManager.getNetcdfFile(request, response, path);
  }

  public boolean isRemote() {
    return isRemote;
  }

  public String getPath() {
    return path;
  }

  static public String getNetcdfFilePath(HttpServletRequest req, String reqPath) throws IOException {
    /*
    if (log.isDebugEnabled()) log.debug("DatasetHandler wants " + reqPath);
    if (debugResourceControl) System.out.println("getNetcdfFile = " + ServletUtil.getRequest(req));

    if (reqPath == null)
      return null;

    if (reqPath.startsWith("/"))
      reqPath = reqPath.substring(1);

    // look for a match
    DataRootManager.DataRootMatch match = dataRootManager.findDataRootMatch(reqPath);

    String fullpath = null;
    if (match != null)
      fullpath = match.dirLocation + match.remaining;
    else {
      String location = dataRootManager.getLocationFromRequestPath(reqPath);
      if (file != null)
        fullpath = file.getPath();
    }
    return fullpath;
  }

    if (reqPath == null)
      return null;

    if (reqPath.startsWith("/"))
      reqPath = reqPath.substring(1);

    // look for a match
    DataRootHandler.DataRootMatch match = datasetManager.findDataRootMatch(reqPath);

    String fullpath = null;
    if (match != null)
      fullpath = match.dirLocation + match.remaining;
    else {
      File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(reqPath);
      if (file != null)
        fullpath = file.getAbsolutePath();
    }
    return fullpath;  */
    return null;
  }

  /*
      @Override
    public FeatureDataset findDatasetByPath(HttpServletRequest req, HttpServletResponse res, String datasetPath) throws IOException {

      FeatureDataset fd = TdsRequestedDataset.getFeatureCollection(req, res, datasetPath);

      if (fd == null) {

        NetcdfFile ncfile = TdsRequestedDataset.getNetcdfFile(req, res, datasetPath); // LOOK should handle this case ??
        if (ncfile != null) {
          //Wrap it into a FeatureDataset
          Set<NetcdfDataset.Enhance> enhance = Collections.unmodifiableSet(EnumSet.of(NetcdfDataset.Enhance.CoordSystems, NetcdfDataset.Enhance.ConvertEnums));
          fd = FeatureDatasetFactoryManager.wrap(
                  FeatureType.ANY,                  // will check FeatureType below if needed...
                  NetcdfDataset.wrap(ncfile, enhance),
                  null,
                  new Formatter(System.err));       // better way to do this?
        }
      }

      return fd;
    }
   */
}
