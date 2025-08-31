Sculk Muffler
=============

A sound muffler block for 1.21.1. Currently, only Neoforge is supported but I
plan on including support for Fabric in a later version.

Rather than muffling specific sounds in a client-side list, this mod adds a
block that muffles all sounds in front of it. You can completely mute them, or
set a volume to make them quieter. The range is adjustable as well.

In addition, this blocks vibrations so sculk sensors and wardens won't be
attracted to muffled sounds. Likewise, the bell's mechanics will be disrupted
when within a zone of silence.

Finally, the Sculk Muffler can be turned off via redstone.


## Changelog

### 1.2

* Added: Support for muffling global sound events (Wither spawning, etc.) by
  converting them to non-global events as though the global sound events game
  rule is disabled.
* Fixed: Updating the volume of AbstractTickableSoundInstance sounds will remember
  the sound's original volume so it can be restored when the muffling state
  changes without the instance's volume being changed.
* Changed: Update the volume of AbstractSoundInstance sounds of the RECORDS category
  every tick so that muffling can apply correctly to jukebox songs.
* Changed: Clamp the volume of a sound between 0% and 100% when applying
  muffling to ensure expected behavior. This should fix jukebox songs.
* Changed: Improve the line rendering when drawing the working area of a muffler.
* 