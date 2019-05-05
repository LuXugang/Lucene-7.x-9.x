package lucene.query.rangeQuery;

import org.apache.lucene.geo.GeoEncodingUtils;

/**
 * @author Lu Xugang
 * @date 2019-05-05 16:31
 */
public class GeoUtilsTest {

  public static void main(String[] args) {
    double latitude1 = 30.23;
    double latitude2 = -30.23;
    int latitude1Encode = GeoEncodingUtils.encodeLatitude(latitude1);
    int latitude2Encode = GeoEncodingUtils.encodeLatitude(latitude2);
    System.out.println(latitude1Encode);
    System.out.println(latitude2Encode);
  }

}
