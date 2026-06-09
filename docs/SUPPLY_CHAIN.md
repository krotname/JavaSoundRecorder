# Supply Chain Verification

This project publishes release artifacts with three verification layers:

- SHA-256 checksums in `checksums.txt`.
- CycloneDX SBOM files in JSON and XML.
- GitHub artifact attestations for build provenance and SBOM linkage.

## Verify release checksums

Download the release assets, then run:

```bash
sha256sum -c checksums.txt
```

## Verify build provenance

Use GitHub CLI against the downloaded release jar:

```bash
gh attestation verify javasoundrecorder-1.0.0-all.jar \
  -R krotname/JavaSoundRecorder
```

## Verify SBOM attestation

The release workflow attests the CycloneDX JSON SBOM as describing the release jar:

```bash
gh attestation verify javasoundrecorder-1.0.0-all.jar \
  -R krotname/JavaSoundRecorder \
  --predicate-type https://cyclonedx.org/bom
```

## Release workflow evidence

`.github/workflows/release.yml` builds the shaded jar, generates CycloneDX SBOM files,
creates checksums, and publishes GitHub artifact attestations before uploading release assets.
