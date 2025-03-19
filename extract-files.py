#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2025 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

import extract_utils.tools
extract_utils.tools.DEFAULT_PATCHELF_VERSION = '0_18'

from extract_utils.fixups_blob import (
    BlobFixupCtx,
    blob_fixup,
    blob_fixups_user_type,
)
from extract_utils.fixups_lib import (
    lib_fixup_remove,
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)

namespace_imports = [
    'device/xiaomi/peridot',
    'hardware/qcom-caf/sm8650',
    'hardware/qcom-caf/bootctrl',
    'hardware/qcom-caf/common/libqti-perfd-client',
    'hardware/qcom-caf/wlan',
    'hardware/lineage/interfaces/power-libperfmgr',
    'hardware/google/interfaces',
    'hardware/google/pixel',
    'hardware/xiaomi',
    'vendor/qcom/opensource/commonsys-intf/display',
    'vendor/qcom/opensource/commonsys/display',
    'vendor/qcom/opensource/dataservices',
]

def lib_fixup_odm_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}_{partition}' if partition == 'odm' else None

def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}_{partition}' if partition == 'vendor' else None


lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    (
        'sqlite3',
    ): lib_fixup_odm_suffix,
    (
        'vendor.qti.diaghal@1.0',
        'vendor.qti.hardware.display.allocator@1.0',
        'vendor.qti.hardware.display.allocator@3.0',
        'vendor.qti.hardware.display.allocator@4.0',
        'vendor.qti.hardware.wifidisplaysession@1.0',
        'vendor.qti.imsrtpservice@3.0',
        'vendor.qti.ImsRtpService-V1-ndk',
        'vendor.qti.qesdhal@1.0',
        'vendor.qti.qesdhal@1.1',
        'vendor.qti.qesdhal@1.2',
        'vendor.qti.qesdhalaidl-V2-ndk',
        'vendor.xiaomi.hardware.fingerprintextension-V1-ndk',
        'vendor.xiaomi.hw.touchfeature-V1-ndk',
    ): lib_fixup_vendor_suffix,
    (
        'libar-pal',
        'liblx-osal',
        'libpalclient',
    ): lib_fixup_remove,
}

blob_fixups: blob_fixups_user_type = {
    ('odm/bin/hw/vendor.xiaomi.sensor.citsensorservice.aidl',
     'vendor/bin/hw/vendor.qti.hardware.display.composer-service',
     'vendor/lib64/libdisplaydebug.so'
    ): blob_fixup()
        .replace_needed('android.hardware.graphics.composer3-V2-ndk.so', 'android.hardware.graphics.composer3-V3-ndk.so'),
    ('odm/etc/camera/enhance_motiontuning.xml',
     'odm/etc/camera/motiontuning.xml',
     'odm/etc/camera/night_motiontuning.xml'): blob_fixup()
        .regex_replace('xml=version', 'xml version'),

    ('odm/lib64/hw/camera.qcom.so',
     'odm/lib64/hw/com.qti.chi.override.so',
     'odm/lib64/hw/camera.xiaomi.so',
     'odm/lib64/libcamxcommonutils.so',
     'odm/lib64/libmialgoengine.so',
     'odm/lib64/libchifeature2.so',
     'vendor/lib64/libcameraopt.so'
     ): blob_fixup()
       .add_needed('libprocessgroup_shim.so'),

    'odm/lib64/libmibokeh_845_video.so': blob_fixup()
       .add_needed('libwrapper_dlengine.so'),

    'odm/lib64/camera/components/com.mi.node.mawsaliency.so': blob_fixup()
       .add_needed('libwrapper_dlengine_shim.so'),

    'odm/lib64/camera/components/com.mi.node.dlengine.so': blob_fixup()
       .add_needed('libwrapper_dlengine_shim.so'),

    'odm/lib64/hw/camera.xiaomi.so': blob_fixup()
        .replace_needed('libui.so', 'libui-v34.so'),

    'odm/lib64/libwrapper_dlengine.so': blob_fixup()
       .add_needed('libwrapper_dlengine_shim.so'),

    ('odm/lib64/camera/com.qti.actuator.peridot_aac_imx882_gt9764ber_wide_i_actuator.so',
     'odm/lib64/camera/com.qti.actuator.peridot_ofilm_imx882_gt9764ber_wide_iii_actuator.so',
     'odm/lib64/camera/com.qti.eeprom.peridot_aac_ov20b40_gt24p64e_front_ii_eeprom.so',
     'odm/lib64/camera/com.qti.actuator.peridot_ofilm_imx882_aw86016csr_wide_ii_actuator.so',
     'odm/lib64/camera/com.qti.eeprom.peridot_aac_imx355_gt24p64e_ultra_i_eeprom.so',
     'odm/lib64/camera/com.qti.eeprom.peridot_aac_imx882_gt24p128f_wide_i_eeprom.so',
     'odm/lib64/camera/com.qti.eeprom.peridot_ofilm_imx355_p24c64e_ultra_ii_eeprom.so',
     'odm/lib64/camera/com.qti.eeprom.peridot_ofilm_imx882_bl24sa128b_wide_ii_eeprom.so',
     'odm/lib64/camera/com.qti.eeprom.peridot_ofilm_imx882_gt24p128f_wide_iii_eeprom.so',
     'odm/lib64/camera/com.qti.eeprom.peridot_ofilm_ov20b40_p24c64e_front_eeprom.so',
     'odm/lib64/camera/com.qti.sensor.peridot_aac_imx355_ultra_i.so',
     'odm/lib64/camera/com.qti.sensor.peridot_aac_imx882_wide_i.so',
     'odm/lib64/camera/com.qti.sensor.peridot_ofilm_ov20b40_front.so',
     'odm/lib64/camera/components/com.qti.hwcfg.bps.so',
     'odm/lib64/camera/components/com.qti.hwcfg.ife.so',
     'odm/lib64/camera/components/com.qti.hwcfg.ipe.so',
     'odm/lib64/com.xiaomi.camx.hook.so',
     'odm/lib64/com.xiaomi.chi.hook.so',
     'odm/lib64/vendor.qti.hardware.camera.postproc@1.0-service-impl.so',
     'odm/lib64/camera/components/com.jigan.node.videobokeh.so',
     'odm/lib64/camera/components/com.mi.node.aiasd.so',
     'odm/lib64/camera/components/com.mi.node.dlengine.so',
     'odm/lib64/camera/components/com.mi.node.mawsaliency.so',
     'odm/lib64/camera/components/com.mi.node.rearvideo.so',
     'odm/lib64/camera/components/com.mi.node.skinbeautifier.so',
     'odm/lib64/camera/components/com.mi.node.videobokeh.so',
     'odm/lib64/camera/components/com.mi.node.videofilter.so',
     'odm/lib64/camera/components/com.mi.node.videonight.so',
     'odm/lib64/camera/components/com.qti.node.aon.so',
     'odm/lib64/camera/components/com.qti.node.depth.so',
     'odm/lib64/camera/components/com.qti.node.depthprovider.so',
     'odm/lib64/camera/components/com.qti.node.dewarp.so',
     'odm/lib64/camera/components/com.qti.node.eisv2.so',
     'odm/lib64/camera/components/com.qti.node.eisv3.so',
     'odm/lib64/camera/components/com.qti.node.evadepth.so',
     'odm/lib64/camera/components/com.qti.node.gme.so',
     'odm/lib64/camera/components/com.qti.node.gyrornn.so',
     'odm/lib64/camera/components/com.qti.node.hdr10pgen.so',
     'odm/lib64/camera/components/com.qti.node.hdr10phist.so',
     'odm/lib64/camera/components/com.qti.node.itofpreprocess.so',
     'odm/lib64/camera/components/com.qti.node.ml.so',
     'odm/lib64/camera/components/com.qti.node.mlinference.so',
     'odm/lib64/camera/components/com.qti.node.pixelstats.so',
     'odm/lib64/camera/components/com.qti.node.seg.so',
     'odm/lib64/camera/components/com.qti.node.swec.so',
     'odm/lib64/camera/components/com.qti.node.swregistration.so',
     'odm/lib64/camera/components/com.qti.stats.cnndriver.so',
     'odm/lib64/camera/components/com.xiaomi.node.smooth_transition.so',
     'odm/lib64/camera/components/libcamxevainterface.so',
     'odm/lib64/camera/components/libdepthmapwrapper_itof.so',
     'odm/lib64/camera/components/libdepthmapwrapper_secure.so',
     'odm/lib64/camera/libchxlogicalcameratable.so',
     'odm/lib64/com.qti.camx.chiiqutils.so',
     'odm/lib64/com.qti.chiusecaseselector.so',
     'odm/lib64/com.qti.feature2.afbrckt.so',
     'odm/lib64/com.qti.feature2.anchorsync.so',
     'odm/lib64/com.qti.feature2.demux.so',
     'odm/lib64/com.qti.feature2.derivedoffline.so',
     'odm/lib64/com.qti.feature2.fusion.so',
     'odm/lib64/com.qti.feature2.generic.so',
     'odm/lib64/com.qti.feature2.gs.sm8650.so',
     'odm/lib64/hw/camera.qcom.sm8650.so',
     'odm/lib64/hw/camera.qcom.so',
     'odm/lib64/hw/com.qti.chi.offline.so',
     'odm/lib64/hw/com.qti.chi.override.so',
     'odm/lib64/com.qti.feature2.hdr.so',
     'odm/lib64/com.qti.feature2.mcreprocrt.so',
     'odm/lib64/com.qti.feature2.memcpy.so',
     'odm/lib64/com.qti.feature2.metadataserializer.so',
     'odm/lib64/com.qti.feature2.mfsr.so',
     'odm/lib64/com.qti.feature2.ml.so',
     'odm/lib64/com.qti.feature2.mux.so',
     'odm/lib64/com.qti.feature2.offlinestatsregeneration.so',
     'odm/lib64/com.qti.feature2.qcfa.so',
     'odm/lib64/com.qti.feature2.rawhdr.so',
     'odm/lib64/com.qti.feature2.realtimeserializer.so',
     'odm/lib64/com.qti.feature2.rt.so',
     'odm/lib64/com.qti.feature2.rtmcx.so',
     'odm/lib64/com.qti.feature2.serializer.so',
     'odm/lib64/com.qti.feature2.statsregeneration.so',
     'odm/lib64/com.qti.feature2.stub.so',
     'odm/lib64/com.qti.feature2.swmf.so',
     'odm/lib64/com.qti.qseeutils.so',
     'odm/lib64/com.qualcomm.mcx.distortionmapper.so',
     'odm/lib64/com.qualcomm.mcx.linearmapper.so',
     'odm/lib64/com.qualcomm.mcx.nonlinearmapper.so',
     'odm/lib64/com.qualcomm.mcx.policy.mfl.so',
     'odm/lib64/com.qualcomm.qti.mcx.usecase.extension.so',
     'odm/lib64/libcamerapostproc.so',
     'odm/lib64/libcamxifestriping.so',
     'odm/lib64/libfastmessage.so',
     'odm/lib64/libhme.so',
     'odm/lib64/libipebpsstriping.so',
     'odm/lib64/libipebpsstriping170.so',
     'odm/lib64/libmctfengine_stub.so',
     'odm/lib64/libmfec.so',
     'odm/lib64/libmmcamera_bestats.so',
     'odm/lib64/libmmcamera_cac.so',
     'odm/lib64/libmmcamera_lscv35.so',
     'odm/lib64/libmmcamera_mfnr.so',
     'odm/lib64/libmmcamera_mfnr_t4.so',
     'odm/lib64/libofflinefeatureintf.so',
     'odm/lib64/libtunningmemhook.so',
     'odm/lib64/libubifocus.so',
     'odm/lib64/vendor.qti.hardware.camera.aon-service-impl.so',
     'odm/lib64/vendor.qti.hardware.camera.offlinecamera-service-impl.so',
     'odm/lib64/libcamxhwnodecontext.so',
     'odm/lib64/libcamximageformatutils.so',
     'odm/lib64/libcamxncsdatafactory.so',
     'odm/lib64/libchifeature2.so',
     'odm/lib64/libcom.xiaomi.mawutilsold.so',
     'odm/lib64/libcommonchiutils.so',
     'odm/lib64/libipebpsstriping480.so',
     'odm/lib64/libisphwsetting.so',
     'odm/lib64/libjpege.so',
     'odm/lib64/libmmcamera_pdpc.so',
     'odm/lib64/libopestriping.so',
     'odm/lib64/libtfestriping.so',
     'vendor/bin/hw/vendor.qti.hardware.display.allocator-service'
    ): blob_fixup()
       .replace_needed('android.hardware.graphics.allocator-V1-ndk.so', 'android.hardware.graphics.allocator-V2-ndk.so'),

    ('odm/lib64/libaudioroute_ext.so',
     'vendor/lib64/libar-pal.so',
     'vendor/lib64/libagm.so'): blob_fixup()
        .replace_needed('libaudioroute.so', 'libaudioroute-v34.so'),

    'vendor/bin/init.qcom.usb.sh': blob_fixup()
        .regex_replace('ro.product.marketname', 'ro.product.odm.marketname'),

     ('vendor/bin/hw/vendor.qti.media.c2@1.0-service',
     'vendor/bin/hw/vendor.qti.media.c2audio@1.0-service'
     ): blob_fixup()
        .add_needed('libcodec2_hidl_shim.so'),

    'vendor/lib64/libqcodec2_core.so': blob_fixup()
        .add_needed('libcodec2_shim.so'),

    'vendor/lib64/hw/audio.primary.pineapple.so': blob_fixup()
        .add_needed('libaudioroute-v34.so'),

    ('vendor/bin/hw/vendor.qti.media.c2@1.0-service',
     'vendor/bin/hw/vendor.qti.media.c2audio@1.0-service'): blob_fixup()
        .add_needed('libcodec2_hidl_shim.so'),

     'vendor/etc/vintf/manifest/c2_manifest_vendor.xml': blob_fixup()
        .regex_replace(r'.+DOLBY.+\n', '')
        .regex_replace(r'.+<!-- DOLBY.+\n', '')
        .regex_replace(r'.+<hal.*name=".*dv.*".*>\n', '')
        .regex_replace(r'.+<hal.*name=".*dolby.*".*>\n', ''),

    'vendor/lib64/libqcrilNr.so|vendor/lib64/libril-db.so': blob_fixup()
        .binary_regex_replace(rb'persist\.vendor\.radio\.poweron_opt', rb'persist.vendor.radio.poweron_ign'),

    'vendor/lib64/vendor.libdpmframework.so': blob_fixup()
        .add_needed('libbinder_shim.so')
        .add_needed('libhidlbase_shim.so'),

    'vendor/lib64/libqcc_sdk.so': blob_fixup()
        .add_needed('libbinder_shim.so'),

    'vendor/lib64/libqms_client.so': blob_fixup()
        .add_needed('libbinder_shim.so'),

    'vendor/bin/mi_thermald': blob_fixup()
        .binary_regex_replace(b'%d/on', b'%d/..'),

    'vendor/bin/xtra-daemon': blob_fixup()
        .add_needed('libbinder_shim.so'),

    ('vendor/lib64/libcne.so',
     'vendor/bin/qms'): blob_fixup()
        .add_needed('libbinder_shim.so'),

    ('vendor/etc/media_codecs.xml',
     'vendor/etc/media_codecs_cliffs_v0.xml',
     'vendor/etc/media_codecs_performance_cliffs_v0.xml'): blob_fixup()
        .regex_replace(
            r'.+media_codecs_(google_audio|google_c2|google_telephony|google_video|vendor_audio|dolby_audio).+\n',
            ''
        )
        .regex_replace(
            r'        <MediaCodec name="c2\.qti\.dv\.(decoder|encoder).*type="video/dolby-vision".*>\n(.*\n)*?        </MediaCodec>\n',
            ''
        )
        .regex_replace(
            r'        <MediaCodec name="c2\.qti\.dv\.decoder\.secure".*type="video/dolby-vision".*>\n(.*\n)*?        </MediaCodec>\n',
            ''
        ),

    'vendor/etc/kvh2xml.xml': blob_fixup()
        .regex_replace(
            r'.+<TAG id="0xc000033" name="dolby_effect_param_tag"/>.*\n',
            ''
        ),

    'vendor/etc/clstc_config_library.xml': blob_fixup()
        .regex_replace(
            r'(<library>\s*<name>libdolbyclstc\.so</name>\s*<priority>1</priority>\s*)<enable>1</enable>',
            r'\1<enable>0</enable>'
        ),

    'product/etc/device_features/peridot.xml': blob_fixup()
        .regex_replace(
            r'<bool name="support_dolby_version_brighten">true</bool>',
            '<bool name="support_dolby_version_brighten">false</bool>'
        )
        .regex_replace(
            r'<bool name="gallery_support_dolby">true</bool>',
            '<bool name="gallery_support_dolby">false</bool>'
        ),

    'vendor/etc/media_cliffs_v0/video_system_specs.json': blob_fixup()
        .regex_replace(
            r'        "DolbyVision": \{\s*"profiles": \[4, 5, 8\],\s*"max_main_tier_level": 12,\s*"max_high_tier_level": 12\s*\},',
            ''
        )
        .regex_replace(
            r'.+//dolby post process.*\n',
            ''
        )
        .regex_replace(
            r'.+"libqcodec2_dolbydecoderfilter\.so",.*\n',
            ''
        ),
}

module = ExtractUtilsModule(
    'peridot',
    'xiaomi',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    add_firmware_proprietary_file=False,
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()
