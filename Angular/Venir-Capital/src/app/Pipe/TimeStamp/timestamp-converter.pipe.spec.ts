import { TimestampConverterPipe } from './timestamp-converter.pipe';

describe('TimestampConverterPipe', () => {
  it('create an instance', () => {
    const pipe = new TimestampConverterPipe();
    expect(pipe).toBeTruthy();
  });
});
