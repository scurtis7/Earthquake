import { definePreset } from '@primeng/themes';
import Aura from '@primeng/themes/aura';

export const ColorPresets = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{sky.50}',
      100: '{sky.100}',
      200: '{sky.200}',
      300: '{sky.300}',
      400: '{sky.400}',
      500: '{sky.500}',
      600: '{sky.600}',
      700: '{sky.700}',
      800: '{sky.800}',
      900: '{sky.900}',
      950: '{sky.950}'
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '{slate.50}',
          100: '{slate.100}',
          200: '{slate.200}',
          300: '{slate.300}',
          400: '{slate.400}',
          500: '{slate.500}',
          600: '{slate.600}',
          700: '{slate.700}',
          800: '{slate.800}',
          900: '{slate.900}',
          950: '{slate.950}'
        }
      },
      dark: {
        surface: {
          0: '#ffffff',
          50: '{sky.50}',
          100: '{sky.100}',
          200: '{sky.200}',
          300: '{sky.300}',
          400: '{sky.400}',
          500: '{sky.500}',
          600: '{sky.600}',
          700: '{sky.700}',
          800: '{sky.800}',
          900: '{sky.900}',
          950: '{sky.950}'
        }
      }
    }
  },
  components: {
    message: {
      colorScheme: {
        // Aura's default dark messages use a faint tint that washes out on the sky surfaces,
        // so use a deeper background and lighter text for readable contrast
        dark: {
          success: {
            background: 'color-mix(in srgb, {green.950}, transparent 20%)',
            borderColor: '{green.600}',
            color: '{green.200}'
          },
          error: {
            background: 'color-mix(in srgb, {red.950}, transparent 20%)',
            borderColor: '{red.600}',
            color: '{red.200}'
          }
        }
      }
    }
  }
});
