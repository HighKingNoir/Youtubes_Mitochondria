import { TestBed } from '@angular/core/testing';

import { JWTinterceptorInterceptor } from './jwtinterceptor.interceptor';

describe('JWTinterceptorInterceptor', () => {
  beforeEach(() => TestBed.configureTestingModule({
    providers: [
      JWTinterceptorInterceptor
      ]
  }));

  it('should be created', () => {
    const interceptor: JWTinterceptorInterceptor = TestBed.inject(JWTinterceptorInterceptor);
    expect(interceptor).toBeTruthy();
  });
});
