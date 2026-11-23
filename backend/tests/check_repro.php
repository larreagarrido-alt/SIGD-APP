<?php
declare(strict_types=1);
if(PHP_SAPI!=='cli')exit(1);
try{
    if(PHP_VERSION_ID<80100)throw new RuntimeException('PHP 8.1+ required');
    foreach(['pdo_mysql','mbstring','openssl'] as $extension){if(!extension_loaded($extension))throw new RuntimeException('Missing extension: '.$extension);}
    if(!filter_var(ini_get('allow_url_fopen'),FILTER_VALIDATE_BOOLEAN)||!is_callable('proc_open'))throw new RuntimeException('Enable loopback HTTP streams and proc_open for the local tests');
    $root=dirname(__DIR__,2);
    $manifest=json_decode(file_get_contents(__DIR__.'/repro_manifest.json'),true,512,JSON_THROW_ON_ERROR);
    foreach($manifest['files'] as $file){
        if(str_contains($file,'..')||!preg_match('#^(?:backend/(?:src|public|tests|bin|config)/|database/|docs/|\.gitignore$)#D',$file)
            ||str_contains($file,'/storage/')||str_ends_with($file,'/config.php'))throw new RuntimeException('Unsafe manifest entry');
        if(!is_file($root.'/'.$file))throw new RuntimeException('Missing reviewed dependency: '.$file);
        if(str_starts_with($file,'backend/tests/')&&str_ends_with($file,'.php')){
            preg_match_all('~(?:require|include)(?:_once)?\s+__DIR__\s*\.\s*[\'"](/[^\'"]+)[\'"]~',file_get_contents($root.'/'.$file),$includes);
            foreach($includes[1] as $include){
                // The physical-device opt-in hook is intentionally outside this source-only suite.
                if($include==='/face_native_fixture.php')continue;
                $dependency='backend/tests'.$include;
                if(!in_array($dependency,$manifest['files'],true))throw new RuntimeException('Unlisted test dependency: '.$dependency);
            }
        }
    }
    require __DIR__.'/test_support.php';labEcKey();
    echo 'READY: '.count($manifest['files'])." reviewed source dependencies; synthetic EC keys work. No database connection made.\n";
}catch(Throwable $e){fwrite(STDERR,$e->getMessage().PHP_EOL);exit(1);}
