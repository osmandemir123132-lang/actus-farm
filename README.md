# 🌵 Aktüs Farmı Modu — Minecraft 1.21.1 (Fabric)

Hardcore dünyanda otomatik çok katlı kaktüs farm inşa eder ve yönetir.

## Farm Yapısı (Her Sütun)

```
Y+10 : Kaktüs  ← 3. kat
Y+9  : İp      ← kaktüs büyüyünce çarpar, kırılır
Y+8  : Kum
Y+7  : Kaktüs  ← 2. kat
Y+6  : İp
Y+5  : Kum
Y+4  : Kaktüs  ← 1. kat
Y+3  : İp
Y+2  : Kum
Y+1  : Kum     ← zemin (sen koyarsın)
```

Sütunlar birbirinden 2 blok arayla dizilir (kaktüsler yanyana duramaz).
Varsayılan boyut: **5×5 = 25 sütun**, **3 kat**

---

## Gereksinimler

- **Minecraft 1.21.1**
- **Fabric Loader 0.16.5+**
- **Fabric API 0.102.0+1.21.1**
- **Java 21**

---

## Derleme

```bash
# Proje klasörüne gir
cd actus-farm-mod

# Gradle wrapper indir (ilk seferinde)
gradle wrapper

# Derle
./gradlew build
```

Çıktı: `build/libs/actus-farmi-1.0.0.jar`

---

## Kurulum

1. `actus-farmi-1.0.0.jar` dosyasını `.minecraft/mods/` klasörüne koy
2. Fabric API'yi de `.minecraft/mods/` içine ekle
3. Minecraft'ı Fabric ile başlat

---

## Kullanım

### Adım 1 — Envantere malzemeleri al
| Malzeme | Adet (5×5, 3 kat) |
|---------|-------------------|
| Kaktüs  | 75                |
| Kum     | 100               |
| İp      | 75                |

### Adım 2 — Konumlan
- Farmın başlamasını istediğin köşeye git
- Ayaklarının altında kum bloğu olsun (zemin seviyesi)

### Adım 3 — Başlat
```
/actusfarmi baslat
```

Mod sırasıyla şunları yapar:
1. Kum zeminleri yerleştirir
2. Her zemin kumunun üstüne kaktüs diker
3. Kaktüsün hizasına (bir üst blok) ip koyar
4. İpin üstüne kum koyar
5. Aynı döngüyü 3 kat boyunca tekrarlar
6. İnşa bittikten sonra hasat moduna geçer — kırılan kaktüsleri yeniden eker

---

## Komutlar

| Komut | Açıklama |
|-------|----------|
| `/actusfarmi baslat` | Farmı başlatır |
| `/actusfarmi durdur` | Farmı durdurur |
| `/actusfarmi durum` | İstatistikleri gösterir |
| `/actusfarmi yardim` | Yardım menüsü |

---

## Notlar

- Farm **server-side** çalışır, shader pack'lerle uyumludur
- Oyuncu farmda durduğu sürece düşen kaktüsler otomatik toplanır
- Envanter dolunca farm çalışmaya devam eder, ama ekim durur
