import com.zerokeyboard.ime.voice.ModelCatalog;
public class CatalogSmoke {
 public static void main(String[] args) {
  assert ModelCatalog.values().length==2;
  assert ModelCatalog.byHash(ModelCatalog.TINY.sha256)==ModelCatalog.TINY;
  assert ModelCatalog.byHash(ModelCatalog.BASE.sha256)==ModelCatalog.BASE;
  assert ModelCatalog.byHash("0".repeat(64))==null;
  assert ModelCatalog.byHash(null)==null;
  assert ModelCatalog.TINY.sha256.matches("[0-9a-f]{64}") && ModelCatalog.BASE.sha256.matches("[0-9a-f]{64}");
  assert !ModelCatalog.TINY.filename.contains(".en") && !ModelCatalog.BASE.filename.contains(".en");
  assert ModelCatalog.TINY.bytes==32152673L;
  assert ModelCatalog.BASE.bytes==59707625L;
  System.out.println("PASS: 9 model catalog smoke assertions");
 }
}
