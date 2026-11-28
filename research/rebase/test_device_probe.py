import unittest
import xml.etree.ElementTree as ET

from device_probe import wait


class DeviceProbeWaitTest(unittest.TestCase):
    def test_permission_leaf_is_a_result(self):
        button = ET.fromstring('<node resource-id="permission_deny_button"/>')
        self.assertIs(wait(lambda: button, seconds=0.1), button)


if __name__ == "__main__":
    unittest.main()
