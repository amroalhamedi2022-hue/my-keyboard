import unittest
import xml.etree.ElementTree as ET
from ime_probe import center, matching

class ProbeHelpers(unittest.TestCase):
    def test_bounds_center(self):
        self.assertEqual(center("[10,20][30,60]"),(20,40))
    def test_invalid_bounds(self):
        for value in ("","[0,0][0,1]","[10,10][2,2]"):
            with self.subTest(value=value), self.assertRaises(ValueError):
                center(value)
    def test_exact_description(self):
        xml=ET.fromstring('<hierarchy><node content-desc="q"/><node content-desc="qw"/></hierarchy>')
        self.assertEqual(len(matching(xml,"q")),1)
        self.assertEqual(matching(xml,"absent"),[])
if __name__=="__main__":
    unittest.main()
