package org.rundeck.app.providers

import grails.testing.gorm.DataTest
import org.rundeck.app.data.model.v1.storage.RundeckStorage
import org.rundeck.app.data.model.v1.storage.SimpleStorageBuilder
import org.rundeck.app.data.providers.storage.GormStorageDataProvider
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil
import rundeck.Storage
import rundeck.services.data.StorageDataService
import spock.lang.Specification

class GormStorageDataProviderSpec extends Specification implements DataTest{
    GormStorageDataProvider provider = new GormStorageDataProvider()

    void setup() {
        mockDomains(Storage)
        mockDataService(StorageDataService)
        provider.storageDataService = applicationContext.getBean(StorageDataService)

    }

    def "Get Data"() {
        given:
        def storage1 = new Storage(namespace: 'other', data: 'abc1'.bytes, name: 'abc', dir: 'def',
                storageMeta: [abc: 'xyz1']).save(flush:true)

        when:
        RundeckStorage res = provider.getData(storage1.id)
        then:
        res.name == 'abc'
        res.dir == 'def'
        res.getNamespace() == 'other'
        res.getData() == 'abc1'.bytes
    }

    def "Create"() {

        when:
        SimpleStorageBuilder data = SimpleStorageBuilder.builder()
                                    .data('abc'.bytes)
                                    .dir("xyz")
                                    .name("abc")
                                    .storageMeta([abc: 'xyz'])
                                    .build()
        def storageId = provider.create(data)
        def newStorage = provider.getData(storageId)
        then:
        provider.findResource(null, null,'xyz') == null
        provider.findResource(null,'xyz','abc') != null
        newStorage.dir == 'xyz'
        newStorage.name == 'abc'
        newStorage.namespace == null
        newStorage.data == 'abc'.bytes
        newStorage.getStorageMeta() == [abc: 'xyz']
    }

    def "Update"() {

        when:
        SimpleStorageBuilder data =  SimpleStorageBuilder.builder()
                                        .data('abc'.bytes)
                                        .dir("xyz")
                                        .name("abc")
                                        .storageMeta([abc: 'xyz'])
                                        .build()

        Long storageId = provider.create(data)
        def storage = provider.getData(storageId)
        def oldStorageName = provider.getData(storageId).name
        SimpleStorageBuilder updateData =  SimpleStorageBuilder.builder()
                .data('def'.bytes)
                .name("updated")
                .namespace("changed")
                .build()
        provider.update(storage, updateData, [def: 'abc'])
        def storage1 = provider.getData(storageId)

        then:
        oldStorageName == 'abc'
        storage1.name == 'updated'
        storage1.dir == ''
        storage1.namespace == 'changed'
        storage1.data == 'def'.bytes
        storage1.getStorageMeta() == [abc: 'xyz', def: 'abc']
    }
    def "DeleteResource ok"() {
        when:
        def storage = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(flush:true)
        new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush:true)

        provider.delete(storage)
        def store1 = Storage.findByNamespaceAndDirAndName(null,'', 'abc')
        then:
        store1 == null
        Storage.get(storage.id) == null
    }

    def "list subdirs"() {
        given:
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(flush: true)
        new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush: true)
        new Storage(data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(flush: true)
        new Storage(data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(flush: true)
        new Storage(data: 'abc3'.bytes, name: 'def', dir: 'zinc/pyx',
                storageMeta: [abc: 'xyz3']).save(flush: true)
        when:
        def res1 = provider.listDirectorySubdirs(null, '')
        def res2 = provider.listDirectorySubdirs(null, 'xyz')
        then:
        res1.size() == 5
        res1.contains(storage1)
        res2.size() == 1
    }

    def "list Directory"() {
        when:
        def storage1 = new Storage(namespace: 'other', data: 'abc1'.bytes, name: 'abc', dir: '',
                storageMeta: [abc: 'xyz1']).save(flush:true)
        new Storage(namespace: 'other',data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush:true)
        new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(flush:true)
        new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(flush:true)
        def res1 = provider.listDirectory(null, '')
        def res2 = provider.listDirectory('other', '')
        then:
        res1 != null
        res1.size() == 0
        res2 != null
        res2.size() == 4
        res2.contains(storage1)
    }
}
