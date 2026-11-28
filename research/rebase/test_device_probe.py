import unittest
import xml.etree.ElementTree as ET

from device_probe import DISCARD, RECORDING, voice_control, wait


class DeviceProbeWaitTest(unittest.TestCase):
    def test_permission_leaf_is_a_result(self):
        button = ET.fromstring('<node resource-id="permission_deny_button"/>')
        self.assertIs(wait(lambda: button, seconds=0.1), button)

    def test_discard_does_not_match_the_symbols_key(self):
        symbols = {"code": -10001, "x": 80, "y": 2191}
        legacy_discard = {"code": -10001, "x": 905, "y": 2177}
        self.assertIsNone(voice_control([symbols], DISCARD))
        self.assertIs(voice_control([symbols, legacy_discard], DISCARD), legacy_discard)
        explicit_discard = {"control": "discard", "x": 905, "y": 2177}
        self.assertIs(voice_control([explicit_discard], DISCARD), explicit_discard)

    def test_recording_flag_is_distinct_from_keyboard_keys(self):
        symbol_page = {"code": -10002, "x": 80, "y": 2191}
        flag = {"code": -10002, "x": 0, "y": 0}
        self.assertIsNone(voice_control([symbol_page], RECORDING))
        self.assertIs(voice_control([symbol_page, flag], RECORDING), flag)

if __name__ == "__main__":
    unittest.main()
